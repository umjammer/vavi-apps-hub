package vavi.apps.hub.ws;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;


// TODO is there no utility for this? too much redundant.
public class WebSocketInitializerServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        try {
            // Retrieve the ServerContainer from the ServletContext attributes.
            ServerContainer container = (ServerContainer) getServletContext().getAttribute(ServerContainer.class.getName());

            // Configure the ServerContainer.
            container.setDefaultMaxTextMessageBufferSize(128 * 1024);

            // Simple registration of your WebSocket endpoints.
            container.addEndpoint(RemotePointingServer.class);

        } catch (DeploymentException x) {
            throw new ServletException(x);
        }
    }
}