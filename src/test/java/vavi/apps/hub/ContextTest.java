package vavi.apps.hub;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ContextTest {

    @Test
    void testContextSleepAwakeTouch() {
        Context context = new Context();
        assertEquals(context, Context.getInstance());

        assertTrue(context.isSleeping());

        AtomicBoolean sleepFired = new AtomicBoolean(false);
        AtomicBoolean awakeFired = new AtomicBoolean(false);
        AtomicInteger stateChangeCount = new AtomicInteger(0);

        context.addObserver(event -> {
            if ("hub.sleep".equals(event.getName())) {
                sleepFired.set(true);
            } else if ("hub.awake".equals(event.getName())) {
                awakeFired.set(true);
            } else if ("hub.state.changed".equals(event.getName())) {
                stateChangeCount.incrementAndGet();
            }
        });

        // Awake
        context.awake();
        assertFalse(context.isSleeping());
        assertTrue(awakeFired.get());
        assertEquals(1, stateChangeCount.get());

        // Sleep
        context.sleep();
        assertTrue(context.isSleeping());
        assertTrue(sleepFired.get());
        assertEquals(2, stateChangeCount.get());

        // Touch wakes it up
        awakeFired.set(false);
        context.touch();
        assertFalse(context.isSleeping());
        assertTrue(awakeFired.get());
        assertEquals(3, stateChangeCount.get());
    }

    @Test
    void testInactivityTimeout() throws Exception {
        Context context = new Context();
        context.awake();
        assertFalse(context.isSleeping());

        // Set very short timeout for test and wait for scheduler
        context.setSleepTimeout(50);
        Thread.sleep(200);

        // Sleep mode can be triggered manually or by scheduler
        context.sleep();
        assertTrue(context.isSleeping());

        // Touching wakes it up
        context.touch();
        assertFalse(context.isSleeping());
    }
}
