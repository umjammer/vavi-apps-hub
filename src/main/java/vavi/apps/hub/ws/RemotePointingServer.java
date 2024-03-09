/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub.ws;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.jetty.websocket.core.exception.WebSocketTimeoutException;
import vavi.apps.hub.ws.JsonCodec.JsonDecoder;
import vavi.apps.hub.ws.JsonCodec.JsonEncoder;
import vavi.util.Debug;


/**
 * RemotePointingServer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-10 nsano initial version <br>
 */
@ServerEndpoint(value = "/ws/pointing",
        decoders = JsonDecoder.class,
        encoders = JsonEncoder.class)
public class RemotePointingServer {

    @OnOpen
    public void onOpen(Session session) {
Debug.println("onOpen");
    }

    @OnMessage
    public ClientData onMessage(ClientData data) {
Debug.println("onMessage " + data.message + ": " + data.mx + ", " + data.my);
        return data;
    }

    @OnError
    public void onError(Throwable t) {
        if (t instanceof WebSocketTimeoutException) {
Debug.println(t.getMessage());
        } else {
Debug.printStackTrace(t);
        }
    }

    @OnClose
    public void onClose(Session session) {
Debug.println("onClose");
    }
}