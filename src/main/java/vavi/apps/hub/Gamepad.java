/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub;

import java.io.IOException;
import java.util.logging.Level;

import net.java.games.input.usb.HidController;
import vavi.games.input.hid4java.spi.Hid4JavaEnvironmentPlugin;
import vavi.games.input.listener.GamepadInputEventListener;
import vavi.util.Debug;
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

    static {
        System.setProperty("net.java.games.input.InputEvent.fillAll", "true");

        System.setProperty("vavi.games.input.listener.period", "100");
        System.setProperty("vavi.games.input.listener.warmup", "500");
    }

    @Property(name = "mid")
    String mid;
    @Property(name = "pid")
    String pid;

    int vendorId;
    int productId;

    @Override
    public void init(Context context) {
        try {
            PropsEntity.Util.bind(this);

            vendorId = Integer.decode(mid);
            productId = Integer.decode(pid);

            Hid4JavaEnvironmentPlugin environment = new Hid4JavaEnvironmentPlugin();
            HidController controller = environment.getController(vendorId, productId);
Debug.println(Level.INFO, controller);

            GamepadInputEventListener listener = new GamepadInputEventListener();
            listener.addObserver(context::fireEventHappened);
            controller.addInputEventListener(listener);

            controller.open();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
