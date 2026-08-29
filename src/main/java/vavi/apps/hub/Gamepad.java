/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import net.java.games.input.ControllerEnvironment;
import net.java.games.input.usb.HidController;
import net.java.games.input.usb.HidControllerEnvironment;
import vavi.games.input.listener.GamepadInputEventListener;
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

    @Override
    public void init(Context context) {
        try {
            PropsEntity.Util.bind(this);

            vendorId = Integer.decode(mid);
            productId = Integer.decode(pid);

            String name = "vavi.games.input.hid4java";
            HidControllerEnvironment environment = (HidControllerEnvironment) ControllerEnvironment.getEnvironmentByName(name);
            HidController controller = environment.getController(vendorId, productId);
logger.log(Level.INFO, controller);

            GamepadInputEventListener listener = new GamepadInputEventListener();
            listener.addObserver(context::fireEventHappened);
            controller.addInputEventListener(listener);

            controller.open();
        } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }
}
