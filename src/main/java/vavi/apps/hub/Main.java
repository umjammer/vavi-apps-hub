/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub;

import java.util.ServiceLoader;

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
        System.setProperty("apple.awt.UIElement", "true"); // eliminate dock icon

        Server server = null;
        try {
            server = new Server(60080);

            ServletContextHandler handler = new ServletContextHandler();
            handler.setContextPath("/");

            // jersey
            ResourceConfig config = new ResourceConfig().packages("vavi.apps.hub.jersey");
            ServletHolder servlet = new ServletHolder(new ServletContainer(config));
            handler.addServlet(servlet, "/*"); // TODO sub path

            // websocket
            JakartaWebSocketServletContainerInitializer.configure(handler, null);
            handler.addServlet(WebSocketInitializerServlet.class, "/ws/*");

            // static
            ResourceHandler rh = new ResourceHandler();
Debug.println("static root: " + System.getProperty("vavi.test.webapp") + ", " + System.getProperty("user.dir"));
            rh.setBaseResource(ResourceFactory.of(rh).newResource(System.getProperty("vavi.test.webapp", "") + "static"));
            rh.setWelcomeFiles("index.html");

            // combine contexts
            Handler.Sequence contexts = new Handler.Sequence();
            contexts.addHandler(rh); // if not first, fail
            contexts.addHandler(handler);

            server.setHandler(contexts);

            //
            Context context = new Context();

            ServiceLoader.load(Plugin.class).forEach(p -> {
                try {
                    p.init(context);
Debug.println("PLUGIN: " + p);
                } catch (Exception e) {
Debug.println("PLUGIN: " + e.getMessage());
                }
            });

            server.start();
Debug.println("server start");
            server.join();
Debug.println("server join");
        } catch (Exception e) {
            Debug.printStackTrace(e);
            throw e;
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

}
