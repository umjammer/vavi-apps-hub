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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.imageio.ImageIO;

import vavi.util.event.GenericEvent;


/**
 * Tray.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-03-25 nsano initial version <br>
 */
public class Tray implements Plugin {

    private static final Logger logger = System.getLogger(Tray.class.getName());

    /** */
    private PopupMenu popup;

    /** TODO generated automatically? */
    MenuItem gamepadItem;

    /** */
    MenuItem sleepToggleItem;

    /** */
    private TrayIcon trayIcon;

    private Context context;

    private String lastBundleId = null;

    void updateTray() {
        if (popup == null || context == null) {
            return;
        }
        if (gamepadItem == null) {
            gamepadItem = new MenuItem();
            popup.insert(gamepadItem, 0);
        }
        if (sleepToggleItem == null) {
            sleepToggleItem = new MenuItem();
            sleepToggleItem.addActionListener(e -> {
                if (context != null) {
                    if (context.isSleeping()) {
                        context.awake();
                    } else {
                        context.sleep();
                    }
                }
            });
            popup.insert(sleepToggleItem, 1);
        }

        boolean sleeping = context.isSleeping();
        if (sleeping) {
            gamepadItem.setLabel("🎮 " + (lastBundleId != null ? lastBundleId : "none") + " (sleep)");
            sleepToggleItem.setLabel("⚡ Awake");
            if (trayIcon != null) {
                trayIcon.setToolTip("HUB (Sleeping)");
            }
        } else {
            gamepadItem.setLabel("🎮 " + (lastBundleId != null ? lastBundleId : "none"));
            sleepToggleItem.setLabel("💤 Sleep");
            if (trayIcon != null) {
                trayIcon.setToolTip("HUB (Active)");
            }
        }
    }

    /** TODO location should be at gamepad plugin */
    void onEvent(GenericEvent event) {
        EventQueue.invokeLater(() -> {
            if (event.getName().equals("gamepad.listener.changed")) {
                lastBundleId = (String) event.getArguments()[0];
            }
            updateTray();
        });
    }

    @Override
    public void init(Context context) {
        this.context = context;
        context.addObserver(this::onEvent);

        EventQueue.invokeLater(() -> {
            // Check if the system tray is supported.
            if (!SystemTray.isSupported()) {
logger.log(Level.DEBUG, "SystemTray is not supported");
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

            updateTray();

logger.log(Level.DEBUG, "SystemTray is set");
        });
    }
}
