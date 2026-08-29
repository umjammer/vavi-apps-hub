package vavi.apps.hub;

import java.io.IOException;
import org.hid4java.HidDevices;
import org.hid4java.HidSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


public class TestGamepadRunner {

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void testExclusiveVsNonExclusive() throws Exception {
        System.out.println("--- 1. Testing Exclusive Open (darwinOpenDevicesNonExclusive = false) ---");
        HidSpecification specExclusive = new HidSpecification();
        specExclusive.setAutoStart(false);
        specExclusive.setAutoShutdown(false);
        specExclusive.darwinOpenDevicesNonExclusive = false;
        HidDevices hd1 = new HidDevices(specExclusive);
        hd1.start();
        var dev1 = hd1.getHidDevice(0x54c, 0x9cc, null);
        if (dev1 != null) {
            try {
                dev1.open();
                System.out.println("Exclusive open: SUCCESS");
                dev1.close();
            } catch (IOException e) {
                System.out.println("Exclusive open: FAILED with " + e.getMessage());
            }
        } else {
            System.out.println("Device not found with exclusive spec");
        }
        hd1.shutdown();

        System.out.println("--- 2. Testing Non-Exclusive Open (darwinOpenDevicesNonExclusive = true) ---");
        HidSpecification specNonExclusive = new HidSpecification();
        specNonExclusive.setAutoStart(false);
        specNonExclusive.setAutoShutdown(false);
        specNonExclusive.darwinOpenDevicesNonExclusive = true;
        HidDevices hd2 = new HidDevices(specNonExclusive);
        hd2.start();
        var dev2 = hd2.getHidDevice(0x54c, 0x9cc, null);
        if (dev2 != null) {
            try {
                dev2.open();
                System.out.println("Non-Exclusive open: SUCCESS");
                dev2.close();
            } catch (IOException e) {
                System.out.println("Non-Exclusive open: FAILED with " + e.getMessage());
            }
        } else {
            System.out.println("Device not found with non-exclusive spec");
        }
        hd2.shutdown();
    }
}
