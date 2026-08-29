/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private static final Logger logger = System.getLogger(Context.class.getName());

    private static Context instance;

    private final GenericSupport observers = new GenericSupport();

    private volatile boolean sleeping = true;

    private volatile long lastActivityTime = System.currentTimeMillis();

    private long sleepTimeout = Long.getLong("vavi.apps.hub.sleep.timeout", 60_000);

    private final ScheduledExecutorService scheduler;

    public Context() {
        instance = this;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Hub-InactivityChecker");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::checkInactivity, 5, 5, TimeUnit.SECONDS);
    }

    public static Context getInstance() {
        return instance;
    }

    private void checkInactivity() {
        if (!sleeping && (System.currentTimeMillis() - lastActivityTime > sleepTimeout)) {
            logger.log(Level.INFO, "Inactivity timeout ({0}ms) reached. Entering sleep mode.", sleepTimeout);
            sleep();
        }
    }

    public boolean isSleeping() {
        return sleeping;
    }

    public synchronized void sleep() {
        if (!sleeping) {
            sleeping = true;
            logger.log(Level.INFO, "Hub is now sleeping");
            fireEventHappened(new GenericEvent(this, "hub.sleep", true));
            fireEventHappened(new GenericEvent(this, "hub.state.changed", true));
        }
    }

    public synchronized void awake() {
        lastActivityTime = System.currentTimeMillis();
        if (sleeping) {
            sleeping = false;
            logger.log(Level.INFO, "Hub is now awake");
            fireEventHappened(new GenericEvent(this, "hub.awake", false));
            fireEventHappened(new GenericEvent(this, "hub.state.changed", false));
        }
    }

    public void touch() {
        lastActivityTime = System.currentTimeMillis();
        if (sleeping) {
            awake();
        }
    }

    public long getSleepTimeout() {
        return sleepTimeout;
    }

    public void setSleepTimeout(long timeout) {
        this.sleepTimeout = timeout;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void addObserver(GenericListener observer) {
        observers.addGenericListener(observer);
    }

    public void fireEventHappened(GenericEvent event) {
        observers.fireEventHappened(event);
    }
}
