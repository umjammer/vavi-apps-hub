/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;


/**
 * RemotePointingServer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-10 nsano initial version <br>
 */
@ServerEndpoint("/pointing")
public class RemotePointingServer {

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("onOpen");
    }

    @OnMessage
    public String onMessage(String message) {
        System.out.println("onMessage " + message);
        return "You said \"" + message + "\".";
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("onClose");
    }
}