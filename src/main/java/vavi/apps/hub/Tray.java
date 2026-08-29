/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.IOException;
import javax.imageio.ImageIO;

import vavi.util.Debug;
import vavi.util.event.GenericEvent;


/**
 * Tray.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-03-25 nsano initial version <br>
 */
public class Tray implements Plugin {

    /** */
    private PopupMenu popup;

    /** TODO generated automatically? */
    MenuItem gamepadItem;

    /** */
    private TrayIcon trayIcon;

    /** TODO location should be at gamepad plugin */
    void gamepad(GenericEvent event) {
        EventQueue.invokeLater(() -> {
            if (popup == null) {
                return;
            }
            if (gamepadItem == null) {
                gamepadItem = new MenuItem();
                popup.insert(gamepadItem, 0);
            }
            if (event.getName().equals("gamepad.listener.changed")) {
                String bundleId = (String) event.getArguments()[0];
                gamepadItem.setLabel("🎮 " + (bundleId != null ? bundleId : "none"));
            }
        });
    }

    @Override
    public void init(Context context) {
        context.addObserver(this::gamepad);

        EventQueue.invokeLater(() -> {
            // Check if the system tray is supported.
            if (!SystemTray.isSupported()) {
Debug.println("SystemTray is not supported");
                return;
            }

            // Get the system tray object.
            SystemTray tray = SystemTray.getSystemTray();

            // Create an image to be displayed in the system tray.
            Image image;
            try {
                image = ImageIO.read(Tray.class.getResourceAsStream("/hub.png"));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            java.awt.Dimension trayDim = tray.getTrayIconSize();
            Image scaledImage = image.getScaledInstance(trayDim.width, trayDim.height, Image.SCALE_SMOOTH);

            // Create a popup menu.
            popup = new PopupMenu();

            // Create a menu item to close the application.
            MenuItem closeItem = new MenuItem("Close");
            closeItem.addActionListener(e -> System.exit(0));
            popup.add(closeItem);

            // Create a TrayIcon object.
            trayIcon = new TrayIcon(scaledImage, "HUB", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("HUB");

            // Add the TrayIcon to the system tray.
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                throw new IllegalStateException(e);
            }

Debug.println("SystemTray is set");
        });
    }
}
