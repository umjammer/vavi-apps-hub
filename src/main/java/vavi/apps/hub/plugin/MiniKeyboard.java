/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub.plugin;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import net.java.games.input.ControllerEnvironment;
import net.java.games.input.ControllerEvent;
import net.java.games.input.ControllerListener;
import net.java.games.input.InputEvent;
import net.java.games.input.InputEventListener;
import net.java.games.input.usb.HidController;
import net.java.games.input.usb.HidControllerEnvironment;
import net.java.games.input.usb.HidInputEvent;
import vavi.apps.hub.Context;
import vavi.apps.hub.Plugin;
import vavi.util.event.GenericEvent;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * Controls "Hosting AU" by a usb mini keyboard.
 * <p>
 * the mini keyboard (4 keys + a knob) is a composite hid device,
 * the keys send keyboard reports (report id 1), the knob sends consumer reports (report id 5).
 * <pre>
 * key 1 ... Up    (0x52) ... Track A1
 * key 2 ... Left  (0x50) ... Track B1
 * key 3 ... Down  (0x51) ... Track C1
 * key 4 ... Right (0x4f) ... Track D1
 * </pre>
 * when a key is pressed, "Hosting AU" is made front most and the leftmost "Key" popup button
 * of it is set to the track.
 * <p>
 * requirements
 * <ul>
 *  <li>"Hosting AU" is running (it may be (re)started after this plugin is initialized)</li>
 *  <li>"Accessibility" permission for the process running this (or its terminal, ide)</li>
 *  <li>"Input Monitoring" permission for the same, for reading the keyboard</li>
 *  <li>system property {@code vavi.games.input.hid4java.darwinOpenDevicesNonExclusive} is true</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-16 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class MiniKeyboard implements Plugin {

    private static final Logger logger = System.getLogger(MiniKeyboard.class.getName());

    @Property(name = "mid2")
    String mid;
    @Property(name = "pid2")
    String pid;

    int vendorId;
    int productId;

    private Context context;
    private HidController controller;
    private MiniKeyboardInputEventListener inputEventListener;
    private boolean controllerOpened = false;

    /** created lazily, because "Hosting AU" may not be running at initialization */
    private HostingAuController hostingAuController;
    /** pid of "Hosting AU" which {@link #hostingAuController} is bound to */
    private int hostingAuPid;

    /** keyboard report id of the mini keyboard */
    static final int REPORT_ID_KEYBOARD = 1;

    /** hid keyboard usage of a mini keyboard key -> menu item of the "Key" popup button */
    static final Map<Integer, String> keyMap = Map.of(
            0x52, "Track A1", // key 1
            0x50, "Track B1", // key 2
            0x51, "Track C1", // key 3
            0x4f, "Track D1"  // key 4
    );

    @Override
    public void init(Context context) {
        this.context = context;
        try {
            PropsEntity.Util.bind(this);

            vendorId = Integer.decode(mid);
            productId = Integer.decode(pid);

            inputEventListener = new MiniKeyboardInputEventListener(this::miniKeyBoardPressed);
            context.addObserver(this::onContextEvent);

            String name = "vavi.games.input.hid4java";
            HidControllerEnvironment environment = (HidControllerEnvironment) ControllerEnvironment.getEnvironmentByName(name);

            environment.addControllerListener(new ControllerListener() {
                @Override
                public void controllerRemoved(ControllerEvent ev) {
                    if (ev.getController() instanceof HidController hidController) {
                        if (hidController.getVendorId() == vendorId && hidController.getProductId() == productId) {
                            logger.log(Level.INFO, "the mini keyboard %s:%s is disconnected".formatted(vendorId, productId));
                            synchronized (MiniKeyboard.this) {
                                controller = null;
                                controllerOpened = false;
                            }
                            context.fireEventHappened(new GenericEvent(this, "minikeyboard.listener.changed", (Object) null));
                        }
                    }
                }

                @Override
                public void controllerAdded(ControllerEvent ev) {
                    if (ev.getController() instanceof HidController hidController) {
                        if (hidController.getVendorId() == vendorId && hidController.getProductId() == productId) {
                            setupController(hidController);
                            logger.log(Level.INFO, "the mini keyboard %s:%s is (re)connected".formatted(vendorId, productId));
                        }
                    }
                }
            });

            try {
                HidController c = environment.getController(vendorId, productId);
                setupController(c);
                logger.log(Level.INFO, controller);
            } catch (NoSuchElementException e) {
                logger.log(Level.DEBUG, "mini keyboard not found initially: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }

    private synchronized void setupController(HidController c) {
        this.controller = c;
        if (c != null && !controllerOpened) {
            c.addInputEventListener(event -> {
                if (context != null && context.isSleeping()) {
                    // pressed keys must be tracked also while sleeping, not to treat a held key as a new press after awake
                    if (!inputEventListener.newlyPressed(event).isEmpty()) {
                        logger.log(Level.INFO, "Key pressed: waking up Hub");
                        context.touch();
                    }
                    return;
                }
                inputEventListener.onInput(event);
            });
            try {
                c.open();
                controllerOpened = true;
                logger.log(Level.INFO, "mini keyboard opened: " + c);
            } catch (IOException e) {
                logger.log(Level.ERROR, "Failed to open controller: " + e.getMessage(), e);
            }
        }
    }

    /**
     * detects newly pressed keys from boot keyboard like reports.
     * <p>
     * keyboard usages (page 0x07) are not mapped to jinput components,
     * so this uses a raw input report by {@link HidInputEvent#getData()}.
     */
    static class MiniKeyboardInputEventListener implements InputEventListener {

        /** pressed keys at the last report */
        private final byte[] last = new byte[6];

        interface Listener {

            void onPressed(int usage);
        }

        private final Listener listener;

        MiniKeyboardInputEventListener(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void onInput(InputEvent inputEvent) {
            newlyPressed(inputEvent).forEach(listener::onPressed);
        }

        /**
         * updates pressed keys state.
         * report: [report id, modifiers, reserved, key1 ... key6]
         *
         * @return usages newly pressed since the last report, empty when the event is not a keyboard report
         */
        List<Integer> newlyPressed(InputEvent inputEvent) {
            if (!(inputEvent instanceof HidInputEvent hidInputEvent)) {
                return List.of();
            }
            byte[] report = hidInputEvent.getData();
            if (report == null || report.length < 9 || (report[0] & 0xff) != REPORT_ID_KEYBOARD) {
                return List.of();
            }
            List<Integer> pressed = new ArrayList<>();
            for (int i = 3; i < 9; i++) {
                int usage = report[i] & 0xff;
                if (usage != 0 && !contains(last, usage)) {
                    pressed.add(usage);
                }
            }
            System.arraycopy(report, 3, last, 0, last.length);
            return pressed;
        }

        private static boolean contains(byte[] keys, int usage) {
            for (byte k : keys) {
                if ((k & 0xff) == usage) return true;
            }
            return false;
        }
    }

    /** accessibility calls block, so don't do it on the hid callback thread */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MiniKeyboard-HostingAu");
        t.setDaemon(true);
        return t;
    });
    /** selecting takes a while, when keys are pressed rapidly only the last one wins */
    private final AtomicReference<String> pending = new AtomicReference<>();

    /** mini keyboard selects a track of "Hosting AU" */
    void miniKeyBoardPressed(int usage) {
        if (context != null) {
            context.touch(); // extends the inactivity timeout
        }
        String track = keyMap.get(usage);
        logger.log(Level.DEBUG, "key: %02x -> %s".formatted(usage, track));
        if (track != null && pending.getAndSet(track) == null) {
            executor.submit(() -> {
                String t;
                while ((t = pending.get()) != null) {
                    try {
                        HostingAuController hac = hostingAuController();
                        if (hac != null) {
                            hac.selectKey(0, t);
                        }
                    } catch (Exception e) {
                        logger.log(Level.ERROR, e.getMessage(), e);
                    }
                    pending.compareAndSet(t, null);
                }
            });
        }
    }

    /**
     * makes sure the controller is bound to the running "Hosting AU".
     * it is (re)created when "Hosting AU" is started or restarted after the last call.
     * called only on the {@link #executor} thread.
     *
     * @return null when "Hosting AU" is not running
     */
    private HostingAuController hostingAuController() throws IOException {
        Optional<Integer> pid = HostingAuController.findPid();
        if (pid.isEmpty()) {
            logger.log(Level.WARNING, HostingAuController.EXECUTABLE + " is not running");
            closeHostingAuController();
            return null;
        }
        if (hostingAuController == null || hostingAuPid != pid.get()) {
            closeHostingAuController();
            hostingAuController = new HostingAuController();
            hostingAuPid = pid.get();
            logger.log(Level.INFO, "bound to " + HostingAuController.EXECUTABLE + ": " + hostingAuPid);
        }
        return hostingAuController;
    }

    private void closeHostingAuController() {
        if (hostingAuController != null) {
            hostingAuController.close();
            hostingAuController = null;
        }
    }

    private void onContextEvent(GenericEvent event) {
    }
}
