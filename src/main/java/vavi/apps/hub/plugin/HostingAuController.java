/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub.plugin;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.ptr.PointerByReference;

import static java.lang.System.getLogger;


/**
 * Controls "Hosting AU" (com.ju-x.Hosting-AU) through the macOS Accessibility API (AXUIElement) via JNA.
 * <p>
 * "Hosting AU" has no menu for its functions, every function is a button on the main window.
 * but those buttons are standard {@code NSPopUpButton}s, so they can be pressed and
 * their popup menu items can be selected by accessibility.
 * <p>
 * the process which runs this (or its responsible process e.g. a terminal, an ide)
 * needs "Accessibility" permission at "System Settings > Privacy &amp; Security".
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-14 nsano initial version <br>
 */
public class HostingAuController {

    private static final Logger logger = getLogger(HostingAuController.class.getName());

    /** executable name of the target application */
    public static final String EXECUTABLE = "Hosting AU_exe";

    /** ApplicationServices (HIServices) accessibility functions */
    interface ApplicationServices extends Library {

        ApplicationServices INSTANCE = Native.load("ApplicationServices", ApplicationServices.class);

        int kAXErrorSuccess = 0;
        int kAXErrorCannotComplete = -25204;

        int kAXValueCGPointType = 1;

        boolean AXIsProcessTrusted();

        Pointer AXUIElementCreateApplication(int pid);

        int AXUIElementCopyAttributeValue(Pointer element, CFStringRef attribute, PointerByReference value);

        int AXUIElementSetAttributeValue(Pointer element, CFStringRef attribute, Pointer value);

        int AXUIElementPerformAction(Pointer element, CFStringRef action);

        int AXUIElementSetMessagingTimeout(Pointer element, float timeoutInSeconds);

        boolean AXValueGetValue(Pointer value, int type, Structure valuePtr);
    }

    @Structure.FieldOrder({"x", "y"})
    public static class CGPoint extends Structure {
        public double x;
        public double y;
    }

    private static final ApplicationServices ax = ApplicationServices.INSTANCE;
    private static final CoreFoundation cf = CoreFoundation.INSTANCE;

    private static final Pointer kCFBooleanTrue = NativeLibrary.getInstance("CoreFoundation")
            .getGlobalVariableAddress("kCFBooleanTrue").getPointer(0);

    /** values of the "Key" popup buttons */
    private static final Pattern KEY_VALUE = Pattern.compile("Off|Track [A-D]\\d|Master \\d");

    /** key assign popups at the top row of the main window */
    private static final int KEY_SLOTS = 16;

    /** AXUIElementRef of the application */
    private final Pointer app;

    /** @throws IOException the application is not running or no accessibility permission */
    public HostingAuController() throws IOException {
        if (!ax.AXIsProcessTrusted()) {
            throw new IOException("accessibility is not permitted for this process");
        }
        int pid = findPid().orElseThrow(() -> new IOException(EXECUTABLE + " is not running"));
logger.log(Level.DEBUG, "pid: " + pid);
        app = ax.AXUIElementCreateApplication(pid);
        ax.AXUIElementSetMessagingTimeout(app, 1f);
    }

    /** @return pid of "Hosting AU" */
    public static Optional<Integer> findPid() {
        return ProcessHandle.allProcesses()
                .filter(p -> p.info().command().map(c -> c.endsWith("/" + EXECUTABLE)).orElse(false))
                .map(p -> (int) p.pid())
                .findFirst();
    }

    /** releases native resources */
    public void close() {
        cf.CFRelease(new CFTypeRef(app));
    }

    /** makes the application front most */
    public void activate() throws IOException {
        CFStringRef attr = CFStringRef.createCFString("AXFrontmost");
        try {
            check(ax.AXUIElementSetAttributeValue(app, attr, kCFBooleanTrue), "AXFrontmost");
        } finally {
            attr.release();
        }
    }

    /**
     * Selects a menu item of the "Key" popup button.
     *
     * @param slot 0 origin index of the "Key" popup buttons (leftmost is 0)
     * @param item a menu item title e.g. "Track A1"
     */
    public void selectKey(int slot, String item) throws IOException {
        activate();

        // the main window is not exposed while the application is not active
        List<Pointer> popups = waitFor(this::keyPopups, l -> l.size() > slot, 2000);
        if (popups.size() <= slot) {
            throw new IOException("no key popup button: " + slot + "/" + popups.size());
        }
        Pointer popup = popups.get(slot);
        if (item.equals(getString(popup, "AXValue"))) {
logger.log(Level.DEBUG, "already selected: " + item);
            return;
        }

        // AXPress on a popup button blocks while the menu is tracked, so ignore time out
        ax.AXUIElementSetMessagingTimeout(popup, 0.2f);
        // a menu left open (e.g. by a previous failure) makes a press ineffective
        children(popup, "AXMenu").forEach(m -> perform(m, "AXCancel"));
        List<Pointer> menus = List.of();
        // a press just after the previous menu closed is sometimes ignored, so retry
        for (int i = 0; i < 3 && menus.isEmpty(); i++) {
            int r = perform(popup, "AXPress");
            if (r != ApplicationServices.kAXErrorSuccess && r != ApplicationServices.kAXErrorCannotComplete) {
                check(r, "AXPress popup");
            }
            menus = waitFor(() -> children(popup, "AXMenu"), l -> !l.isEmpty(), 700);
logger.log(Level.TRACE, "press: " + i + ", menus: " + menus.size());
        }
        if (menus.isEmpty()) {
            throw new IOException("popup menu is not shown");
        }
        Pointer menu = menus.get(0);
        Optional<Pointer> menuItem = children(menu, "AXMenuItem").stream()
                .filter(e -> item.equals(getString(e, "AXTitle")))
                .findFirst();
        if (menuItem.isEmpty()) {
            perform(menu, "AXCancel");
            throw new IllegalArgumentException("no such menu item: " + item);
        }
        check(perform(menuItem.get(), "AXPress"), "AXPress menu item");

        String value = waitFor(() -> getString(popup, "AXValue"), item::equals, 1000);
logger.log(Level.DEBUG, "key[" + slot + "]: " + value);
        if (!item.equals(value)) {
            throw new IOException("failed to select: " + item + ", current: " + value);
        }
    }

    /**
     * @param slot 0 origin index of the "Key" popup buttons (leftmost is 0)
     * @return current menu item title of the "Key" popup button
     */
    public String keyPopupValue(int slot) throws IOException {
        activate();
        List<Pointer> popups = waitFor(this::keyPopups, l -> l.size() > slot, 2000);
        if (popups.size() <= slot) {
            throw new IOException("no key popup button: " + slot + "/" + popups.size());
        }
        return getString(popups.get(slot), "AXValue");
    }

    /** @return "Key" popup buttons, sorted from left */
    List<Pointer> keyPopups() {
        List<Pointer> popups = new ArrayList<>();
        for (Pointer window : getArray(app, "AXWindows")) {
            for (Pointer popup : children(window, "AXPopUpButton")) {
                String value = getString(popup, "AXValue");
                if (value != null && KEY_VALUE.matcher(value).matches()) {
                    popups.add(popup);
                }
            }
            if (popups.size() >= KEY_SLOTS) {
                break;
            }
            popups.clear();
        }
        // the top row
        double top = popups.stream().mapToDouble(p -> position(p).y).min().orElse(0);
        return popups.stream()
                .filter(p -> Math.abs(position(p).y - top) < 1)
                .sorted(Comparator.comparingDouble(p -> position(p).x))
                .toList();
    }

    // ---- accessibility utilities ----

    private interface Supplier<T> {
        T get() throws IOException;
    }

    /** polls until the condition is satisfied or timed out, returns the last value */
    private static <T> T waitFor(Supplier<T> s, java.util.function.Predicate<T> condition, long timeoutMillis) throws IOException {
        long limit = System.currentTimeMillis() + timeoutMillis;
        T value = s.get();
        while (!condition.test(value) && System.currentTimeMillis() < limit) {
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            value = s.get();
        }
        return value;
    }

    private static void check(int r, String message) throws IOException {
        if (r != ApplicationServices.kAXErrorSuccess) {
            throw new IOException(message + ": AXError " + r);
        }
    }

    private static int perform(Pointer element, String action) {
        CFStringRef a = CFStringRef.createCFString(action);
        try {
            return ax.AXUIElementPerformAction(element, a);
        } finally {
            a.release();
        }
    }

    /** @return +1 retained value or null, the caller must release it */
    private static Pointer copy(Pointer element, String attribute) {
        CFStringRef attr = CFStringRef.createCFString(attribute);
        try {
            PointerByReference value = new PointerByReference();
            int r = ax.AXUIElementCopyAttributeValue(element, attr, value);
            return r == ApplicationServices.kAXErrorSuccess ? value.getValue() : null;
        } finally {
            attr.release();
        }
    }

    private static String getString(Pointer element, String attribute) {
        Pointer value = copy(element, attribute);
        if (value == null) {
            return null;
        }
        try {
            if (!cf.CFGetTypeID(value).equals(CoreFoundation.STRING_TYPE_ID)) {
                return null;
            }
            return new CFStringRef(value).stringValue();
        } finally {
            cf.CFRelease(new CFTypeRef(value));
        }
    }

    /**
     * elements in the returned list are not released, they are leaked intentionally
     * because this is a short-lived test utility.
     */
    private static List<Pointer> getArray(Pointer element, String attribute) {
        List<Pointer> result = new ArrayList<>();
        Pointer value = copy(element, attribute);
        if (value == null) {
            return result;
        }
        if (!cf.CFGetTypeID(value).equals(CoreFoundation.ARRAY_TYPE_ID)) {
            cf.CFRelease(new CFTypeRef(value));
            return result;
        }
        CFArrayRef array = new CFArrayRef(value);
        int count = array.getCount();
        for (int i = 0; i < count; i++) {
            Pointer e = array.getValueAtIndex(i);
            cf.CFRetain(new CFTypeRef(e));
            result.add(e);
        }
        array.release();
        return result;
    }

    private static List<Pointer> children(Pointer element, String role) {
        return getArray(element, "AXChildren").stream()
                .filter(e -> role.equals(getString(e, "AXRole")))
                .toList();
    }

    private static CGPoint position(Pointer element) {
        CGPoint point = new CGPoint();
        Pointer value = copy(element, "AXPosition");
        if (value != null) {
            ax.AXValueGetValue(value, ApplicationServices.kAXValueCGPointType, point);
            point.read();
            cf.CFRelease(new CFTypeRef(value));
        }
        return point;
    }
}
