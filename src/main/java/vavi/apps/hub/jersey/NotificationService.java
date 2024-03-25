/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub.jersey;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import vavi.util.Debug;


/**
 * NotificationService.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-12 nsano initial version <br>
 */
@Path("notification")
public class NotificationService {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("notify")
    public void say(@QueryParam("message") String message,
                    @QueryParam("title") String title,
                    @QueryParam("from") String from) {
        try {
Debug.println("message " + message);
            String subTitle = "From: " + from;
            String sound = "Frog";
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("AppleScriptRococoa");

            String script = String.format(
                    "display notification \"%s\" with title \"%s\" subtitle \"%s\" sound name \"%s\"",
                    message, title, subTitle, sound);
            Object r = engine.eval(script);
Debug.println("script: " + script);
Debug.println("result: " + r);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}