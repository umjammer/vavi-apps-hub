/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub.ws;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.jetty.websocket.core.exception.WebSocketTimeoutException;
import vavi.apps.hub.Context;


/**
 * Tablet emulator server.
 * <p>
 * Receives Apple Pencil samples from Safari on iPad (static/tablet.html)
 * and posts them as macOS tablet events, so GIMP, Photoshop etc. get pressure and tilt.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-11 nsano initial version <br>
 */
@ServerEndpoint("/ws/tablet")
public class TabletServer {

    private static final Logger logger = System.getLogger(TabletServer.class.getName());

    private static final Gson gson = new Gson();

    /** shared by all sessions, there is only one system cursor */
    private static TabletPoster poster;

    private static synchronized TabletPoster poster() {
        if (poster == null) {
            poster = new TabletPoster();
        }
        return poster;
    }

    @OnOpen
    public void onOpen(Session session) {
        Context context = Context.getInstance();
        if (context != null) {
            context.touch();
        }
        session.setMaxIdleTimeout(0);
logger.log(Level.DEBUG, "onOpen: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message) {
        Context context = Context.getInstance();
        if (context != null) {
            context.touch();
        }
        try {
            TabletData data = gson.fromJson(message, TabletData.class);
logger.log(Level.TRACE, data);
            poster().post(data);
        } catch (JsonSyntaxException e) {
logger.log(Level.WARNING, "bad message: " + message);
        }
    }

    @OnError
    public void onError(Throwable t) {
        if (t instanceof WebSocketTimeoutException) {
logger.log(Level.ERROR, t.getMessage());
        } else {
logger.log(Level.ERROR, t.getMessage(), t);
        }
    }

    @OnClose
    public void onClose(Session session) {
        // don't leave the button pressed
        poster().release(null);
logger.log(Level.DEBUG, "onClose: " + session.getId());
    }
}
