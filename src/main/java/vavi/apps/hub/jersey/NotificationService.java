/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub.jersey;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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

    private static final Logger logger = System.getLogger(NotificationService.class.getName());

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("notify")
    public void say(@QueryParam("message") String message,
                    @QueryParam("title") String title,
                    @QueryParam("from") String from) {
        vavi.apps.hub.Context context = vavi.apps.hub.Context.getInstance();
        if (context != null) {
            context.touch();
        }
        try {
logger.log(Level.DEBUG, "message " + message);
            String subTitle = "From: " + from;
            String sound = "Frog";
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("AppleScriptRococoa");

            String script = String.format(
                    "display notification \"%s\" with title \"%s\" subtitle \"%s\" sound name \"%s\"",
                    message, title, subTitle, sound);
            Object r = engine.eval(script);
logger.log(Level.DEBUG, "script: " + script);
logger.log(Level.DEBUG, "result: " + r);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}