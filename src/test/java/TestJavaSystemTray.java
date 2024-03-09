/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;

import vavi.util.Debug;


/**
 * TestJavaSystemTray.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-26 nsano initial version <br>
 */
public class TestJavaSystemTray {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        // Check if the system tray is supported.
        if (!SystemTray.isSupported()) {
Debug.println("SystemTray is not supported");
            return;
        }

        // Get the system tray object.
        SystemTray tray = SystemTray.getSystemTray();

        // Create an image to be displayed in the system tray.
        // WORKS: the icon pass must be correct
        Image image = Toolkit.getDefaultToolkit().getImage("src/main/resources/duke.png");

        // Create a popup menu.
        PopupMenu popup = new PopupMenu();

        // Create a menu item to close the application.
        MenuItem closeItem = new MenuItem("Close");
        closeItem.addActionListener(e -> System.exit(0));
        popup.add(closeItem);

        // Create a TrayIcon object.
        TrayIcon trayIcon = new TrayIcon(image, "SystemTray Demo", popup);

        // Add the TrayIcon to the system tray.
        tray.add(trayIcon);

        // Set the TrayIcon properties.
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("This is a system tray icon.");
Debug.println("SystemTray is set");
    }
}
