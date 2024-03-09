/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub;

import java.io.IOException;

import net.java.games.input.Event;
import net.java.games.input.WrappedComponent;
import net.java.games.input.usb.HidController;
import net.java.games.input.usb.parser.Field;
import vavi.games.input.hid4java.spi.Hid4JavaEnvironmentPlugin;


/**
 * Gamepad.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-03-06 nsano initial version <br>
 */
public class Gamepad {

    public void init() throws IOException {
        int vendorId = 0x54c;
        int productId = 0x9cc;
        Hid4JavaEnvironmentPlugin environment = new Hid4JavaEnvironmentPlugin();
        HidController controller = environment.getController(vendorId, productId);
        controller.addInputEventListener(e -> {
            Event event = new Event();
            while (e.getNextEvent(event)) {
                Field field = ((WrappedComponent<Field>) event.getComponent()).getWrappedObject();
            }
        });
        controller.open();
    }
}
