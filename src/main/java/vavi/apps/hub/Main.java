/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub;

import java.net.URI;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;


/**
 * Main.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-10 nsano initial version <br>
 */
public class Main {

    /** */
    public static void main(String[] args) throws Exception {
        Server server = null;
        try {
            ResourceConfig config = new ResourceConfig().packages("vavi.apps.hub.jersey");

            server = JettyHttpContainerFactory.createServer(
                    URI.create("http://localhost:60080/"), config); // TODO ssl

            // TODO cannot coexists
//            ServletContextHandler handler = new ServletContextHandler(server, "/hub");
//            server.setHandler(handler);
//
//            JakartaWebSocketServletContainerInitializer.configure(handler, null);
//
//            server.setHandler(handler);


            server.start();
            server.join();
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }
}
