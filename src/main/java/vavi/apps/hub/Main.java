/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import javax.imageio.ImageIO;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import vavi.apps.hub.ws.WebSocketInitializerServlet;
import vavi.util.Debug;


/**
 * Main.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-10 nsano initial version <br>
 */
public class Main {

    /** */
    public static void main(String[] args) throws Exception {
        System.setProperty("apple.awt.UIElement", "true");

        Server server = null;
        try {
            server = new Server(60080);

            ServletContextHandler context = new ServletContextHandler();
            context.setContextPath("/");

            // jersey
            ResourceConfig config = new ResourceConfig().packages("vavi.apps.hub.jersey");
            ServletHolder servlet = new ServletHolder(new ServletContainer(config));
            context.addServlet(servlet, "/*"); // TODO sub path

            // websocket
            JakartaWebSocketServletContainerInitializer.configure(context, null);
            context.addServlet(WebSocketInitializerServlet.class, "/ws/*");

            // static
            ResourceHandler rh = new ResourceHandler();
Debug.println("static root: " + System.getProperty("vavi.test.webapp"));
            rh.setBaseResource(ResourceFactory.of(rh).newResource(System.getProperty("vavi.test.webapp") + "static"));
            rh.setWelcomeFiles("index.html");

            // combine contexts
            Handler.Sequence contexts = new Handler.Sequence();
            contexts.addHandler(rh); // if not first, fail
            contexts.addHandler(context);

            server.setHandler(contexts);

            //
            Main app = new Main();
            app.tray();

            server.start();
            server.join();
        } catch (Exception e) {
            Debug.printStackTrace(e);
            throw e;
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    void tray() throws Exception {
        // Check if the system tray is supported.
        if (!SystemTray.isSupported()) {
Debug.println("SystemTray is not supported");
            return;
        }

        // Get the system tray object.
        SystemTray tray = SystemTray.getSystemTray();

        // Create an image to be displayed in the system tray.
        Image image = ImageIO.read(Main.class.getResourceAsStream("/hub.png"));

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
