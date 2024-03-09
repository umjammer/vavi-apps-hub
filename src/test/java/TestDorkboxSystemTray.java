/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;


/**
 * dorkbox SystemTray.
 *
 * works! (using jdk's one internally)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-26 nsano initial version <br>
 */
public class TestDorkboxSystemTray {

    /**
     * @param args
     */
    public static void main(String[] args) {
        SystemTray systemTray = SystemTray.get();
        if (systemTray == null) {
            throw new RuntimeException("Unable to load SystemTray!");
        }

        systemTray.setImage("src/main/resources/duke.png");
        systemTray.setStatus("Not Running");

        systemTray.getMenu().add(new MenuItem("Quit", e -> systemTray.shutdown())).setShortcut('q');
    }
}
