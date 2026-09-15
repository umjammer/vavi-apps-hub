/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub.sandbox;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hid4java.HidDevice;
import org.hid4java.HidDeviceEvent;
import org.hid4java.HidDevices;
import org.hid4java.HidSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import vavi.apps.hub.plugin.HostingAuController;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


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
 *  <li>"Hosting AU" is running</li>
 *  <li>"Accessibility" permission for the process running this test (or its terminal, ide)</li>
 *  <li>"Input Monitoring" permission for the same, for reading the keyboard</li>
 * </ul>
 * system properties
 * <ul>
 *  <li>{@code vavi.test} ... "ide" enables interactive tests</li>
 *  <li>{@code duration} ... seconds to run interactive tests, default 60</li>
 *  <li>{@code exclusive} ... opens the mini keyboard exclusively
 *      (key strokes are not sent to other applications), default false.
 *      macos refuses seizing a keyboard by a non-root process (kIOReturnNotPermitted)</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-14 nsano initial version <br>
 */
@EnabledOnOs(OS.MAC)
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class MiniKeyboardHostingAuTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static boolean hostingAuRunning() {
        return HostingAuController.findPid().isPresent();
    }

    @Property(name = "mid2")
    String mid;
    @Property(name = "pid2")
    String pid;

    int vendorId;
    int productId;

    HidDevices hidDevices;

    /** keyboard report id of the mini keyboard */
    static final int REPORT_ID_KEYBOARD = 1;

    /** hid keyboard usage of a mini keyboard key -> menu item of the "Key" popup button */
    static final Map<Integer, String> keyMap = Map.of(
            0x52, "Track A1", // key 1
            0x50, "Track B1", // key 2
            0x51, "Track C1", // key 3
            0x4f, "Track D1"  // key 4
    );

    @BeforeEach
    void setup() throws Exception {
        PropsEntity.Util.bind(this);

        vendorId = Integer.decode(mid);
        productId = Integer.decode(pid);

        HidSpecification hidSpecification = new HidSpecification();
        hidSpecification.setAutoStart(false);
        hidSpecification.setAutoShutdown(false);
        hidSpecification.darwinOpenDevicesNonExclusive = !Boolean.parseBoolean(System.getProperty("exclusive", "false"));

        hidDevices = new HidDevices(hidSpecification);
        hidDevices.start();
    }

    @AfterEach
    void teardown() throws Exception {
        hidDevices.shutdown();
    }

    /** the keyboard interface (usage page: generic desktop, usage: keyboard) */
    HidDevice getKeyboard() throws Exception {
        return hidDevices.getHidDevices().stream()
                .filter(d -> d.getVendorId() == vendorId && d.getProductId() == productId)
                .filter(d -> d.getUsagePage() == 0x01 && d.getUsage() == 0x06)
                .findFirst()
                .orElse(null);
    }

    static long duration() {
        return TimeUnit.SECONDS.toMillis(Long.getLong("duration", 60));
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    @DisplayName("dump input reports of the mini keyboard")
    void test0() throws Exception {
        HidDevice device = getKeyboard();
        assertNotNull(device);
Debug.println(device);
        device.addInputReportListener(e -> {
Debug.println("id: " + e.getReportId() + "\n" + StringUtil.getDump(e.getReport(), e.getLength()));
        });
        device.open();

        Thread.sleep(duration());

        device.close();
    }

    @Test
    @EnabledIf("hostingAuRunning")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    @DisplayName("select a track of the \"Key\" popup button by accessibility")
    void test1() throws Exception {
        HostingAuController controller = new HostingAuController();
        try {
            String current = controller.keyPopupValue(0);
Debug.println("current: " + current);
            String track = "Track A1".equals(current) ? "Track B1" : "Track A1";
            controller.selectKey(0, track);
            assertEquals(track, controller.keyPopupValue(0));
            controller.selectKey(0, current);
            assertEquals(current, controller.keyPopupValue(0));
        } finally {
            controller.close();
        }
    }

    /** detects newly pressed keys from boot keyboard like reports */
    static class KeyPressDetector {

        /** pressed keys at the last report */
        private final byte[] last = new byte[6];

        interface Listener {
            void onPressed(int usage);
        }

        private final Listener listener;

        KeyPressDetector(Listener listener) {
            this.listener = listener;
        }

        /** report: [report id, modifiers, reserved, key1 ... key6] */
        void onInputReport(HidDeviceEvent event) {
            if (event.getReportId() != REPORT_ID_KEYBOARD || event.getLength() < 9) {
                return;
            }
            byte[] report = event.getReport();
            for (int i = 3; i < 9; i++) {
                int usage = report[i] & 0xff;
                if (usage != 0 && !contains(last, usage)) {
                    listener.onPressed(usage);
                }
            }
            System.arraycopy(report, 3, last, 0, last.length);
        }

        private static boolean contains(byte[] keys, int usage) {
            for (byte k : keys) {
                if ((k & 0xff) == usage) return true;
            }
            return false;
        }
    }

    @Test
    @EnabledIf("hostingAuRunning")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    @DisplayName("mini keyboard selects a track of \"Hosting AU\"")
    void test2() throws Exception {
        HidDevice device = getKeyboard();
        assertNotNull(device);
Debug.println(device);

        HostingAuController controller = new HostingAuController();
        // accessibility calls block, so don't do it on the hid callback thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // selecting takes a while, when keys are pressed rapidly only the last one wins
        AtomicReference<String> pending = new AtomicReference<>();

        KeyPressDetector detector = new KeyPressDetector(usage -> {
            String track = keyMap.get(usage);
Debug.printf("key: %02x -> %s", usage, track);
            if (track != null && pending.getAndSet(track) == null) {
                executor.submit(() -> {
                    String t;
                    while ((t = pending.get()) != null) {
                        try {
                            controller.selectKey(0, t);
                        } catch (Exception e) {
                            Debug.printStackTrace(e);
                        }
                        pending.compareAndSet(t, null);
                    }
                });
            }
        });
        device.addInputReportListener(detector::onInputReport);
        device.open();

        Thread.sleep(duration());

        device.close();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        controller.close();
    }
}
