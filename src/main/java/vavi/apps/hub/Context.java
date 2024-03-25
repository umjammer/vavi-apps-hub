/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub;

import vavi.util.event.GenericEvent;
import vavi.util.event.GenericListener;
import vavi.util.event.GenericSupport;


/**
 * Context.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-03-25 nsano initial version <br>
 */
public class Context {

    private final GenericSupport observers = new GenericSupport();

    public void addObserver(GenericListener observer) {
        observers.addGenericListener(observer);
    }

    public void fireEventHappened(GenericEvent event) {
        observers.fireEventHappened(event);
    }
}
