/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.NoSuchElementException;

import net.java.games.input.ControllerEnvironment;
import net.java.games.input.ControllerEvent;
import net.java.games.input.ControllerListener;
import net.java.games.input.usb.HidController;
import net.java.games.input.usb.HidControllerEnvironment;
import vavi.games.input.listener.GamepadInputEventListener;
import vavi.util.event.GenericEvent;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * Gamepad.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-03-06 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class Gamepad implements Plugin {

    private static final Logger logger = System.getLogger(Gamepad.class.getName());

    static {
        System.setProperty("net.java.games.input.InputEvent.fillAll", "true");
        System.setProperty("net.java.games.input.ControllerEnvironment.excludes", "net.java.games.input");

        System.setProperty("vavi.games.input.listener.period", "100");
        System.setProperty("vavi.games.input.listener.warmup", "500");
    }

    @Property(name = "mid")
    String mid;
    @Property(name = "pid")
    String pid;

    int vendorId;
    int productId;

    private Context context;
    private HidController controller;
    private GamepadInputEventListener inputEventListener;
    private boolean controllerOpened = false;
    private String currentAppBundleId = null;

    @Override
    public void init(Context context) {
        this.context = context;
        try {
            PropsEntity.Util.bind(this);

            vendorId = Integer.decode(mid);
            productId = Integer.decode(pid);

            inputEventListener = new GamepadInputEventListener();
            inputEventListener.addObserver(this::onGamepadListenerEvent);
            context.addObserver(this::onContextEvent);

            String name = "vavi.games.input.hid4java";
            HidControllerEnvironment environment = (HidControllerEnvironment) ControllerEnvironment.getEnvironmentByName(name);

            environment.addControllerListener(new ControllerListener() {
                @Override
                public void controllerRemoved(ControllerEvent ev) {
                    if (ev.getController() instanceof HidController hidController) {
                        if (hidController.getVendorId() == vendorId && hidController.getProductId() == productId) {
                            logger.log(Level.INFO, "the controller %s:%s is disconnected".formatted(vendorId, productId));
                            controller = null;
                            controllerOpened = false;
                            context.fireEventHappened(new GenericEvent(this, "gamepad.listener.changed", (Object) null));
                        }
                    }
                }

                @Override
                public void controllerAdded(ControllerEvent ev) {
                    if (ev.getController() instanceof HidController hidController) {
                        if (hidController.getVendorId() == vendorId && hidController.getProductId() == productId) {
                            setupController(hidController);
                            logger.log(Level.INFO, "the controller %s:%s is (re)connected".formatted(vendorId, productId));
                            if (currentAppBundleId != null) {
                                context.touch();
                            }
                        }
                    }
                }
            });

            try {
                HidController c = environment.getController(vendorId, productId);
                setupController(c);
logger.log(Level.INFO, controller);
            } catch (NoSuchElementException e) {
logger.log(Level.DEBUG, "gamepad not found initially: " + e.getMessage());
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
                    net.java.games.input.Event ev = new net.java.games.input.Event();
                    while (event.getNextEvent(ev)) {
                        float v = ev.getValue();
                        String name = ev.getComponent().getName();
                        if (name.startsWith("Button") && v != 0) {
                            logger.log(Level.INFO, "Button pressed: waking up Hub");
                            context.touch();
                            break;
                        } else if (("X".equals(name) || "Y".equals(name) || "Z".equals(name) || "RZ".equals(name)) && Math.abs(v - 128) > 40) {
                            logger.log(Level.INFO, "Stick moved: waking up Hub");
                            context.touch();
                            break;
                        }
                    }
                    return;
                }
                inputEventListener.onInput(event);
            });
            try {
                c.open();
                controllerOpened = true;
                logger.log(Level.INFO, "Controller opened: " + c);
            } catch (IOException e) {
                logger.log(Level.ERROR, "Failed to open controller: " + e.getMessage(), e);
            }
        }
    }

    private void onGamepadListenerEvent(GenericEvent event) {
        if (event.getName().equals("gamepad.listener.changed")) {
            String bundleId = (String) event.getArguments()[0];
            currentAppBundleId = bundleId;
            if (bundleId != null) {
                logger.log(Level.INFO, "Target app activated: " + bundleId);
                context.touch();
            } else {
                logger.log(Level.INFO, "Target app deactivated");
                context.sleep();
            }
            context.fireEventHappened(event);
        }
    }

    private void onContextEvent(GenericEvent event) {
    }
}
