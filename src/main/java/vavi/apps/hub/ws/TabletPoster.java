/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub.ws;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;


/**
 * Posts synthetic tablet (pen) events to macOS via CoreGraphics.
 * <p>
 * Apps like GIMP (GTK quartz backend) or Photoshop read pressure / tilt
 * from mouse events whose subtype is {@code kCGEventMouseSubtypeTabletPoint},
 * and recognize the pen device by {@code kCGEventTabletProximity} events.
 * So no app specific plugin is needed, this works system wide.
 * <p>
 * system properties
 * <ul>
 *  <li>{@code vavi.apps.hub.tablet.area} ... "x,y,w,h" target screen area in global points (default: main screen)</li>
 *  <li>{@code vavi.apps.hub.tablet.keepAspect} ... fit the tablet aspect ratio into the area (default: true)</li>
 * </ul>
 * needs "Accessibility" permission.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-11 nsano initial version <br>
 */
public class TabletPoster {

    private static final Logger logger = System.getLogger(TabletPoster.class.getName());

    // CGEventType
    static final int kCGEventLeftMouseDown = 1;
    static final int kCGEventLeftMouseUp = 2;
    static final int kCGEventRightMouseDown = 3;
    static final int kCGEventRightMouseUp = 4;
    static final int kCGEventMouseMoved = 5;
    static final int kCGEventLeftMouseDragged = 6;
    static final int kCGEventRightMouseDragged = 7;
    static final int kCGEventTabletProximity = 24;

    // CGEventField
    static final int kCGMouseEventClickState = 1;
    static final int kCGMouseEventPressure = 2;
    static final int kCGMouseEventButtonNumber = 3;
    static final int kCGMouseEventSubtype = 7;
    static final int kCGTabletEventPointX = 15;
    static final int kCGTabletEventPointY = 16;
    static final int kCGTabletEventPointButtons = 18;
    static final int kCGTabletEventPointPressure = 19;
    static final int kCGTabletEventTiltX = 20;
    static final int kCGTabletEventTiltY = 21;
    static final int kCGTabletEventRotation = 22;
    static final int kCGTabletEventDeviceID = 24;
    static final int kCGTabletProximityEventVendorID = 28;
    static final int kCGTabletProximityEventTabletID = 29;
    static final int kCGTabletProximityEventPointerID = 30;
    static final int kCGTabletProximityEventDeviceID = 31;
    static final int kCGTabletProximityEventSystemTabletID = 32;
    static final int kCGTabletProximityEventVendorPointerType = 33;
    static final int kCGTabletProximityEventVendorPointerSerialNumber = 34;
    static final int kCGTabletProximityEventVendorUniqueID = 35;
    static final int kCGTabletProximityEventCapabilityMask = 36;
    static final int kCGTabletProximityEventPointerType = 37;
    static final int kCGTabletProximityEventEnterProximity = 38;

    // CGEventMouseSubtype
    static final int kCGEventMouseSubtypeTabletPoint = 1;
    static final int kCGEventMouseSubtypeTabletProximity = 2;

    // NSPointingDeviceType
    static final int NSPointingDeviceTypePen = 1;

    // capability mask (NX_TABLET_CAPABILITY_*)
    static final int CAPABILITY_ABSX = 0x0002;
    static final int CAPABILITY_ABSY = 0x0004;
    static final int CAPABILITY_BUTTONS = 0x0040;
    static final int CAPABILITY_TILTX = 0x0080;
    static final int CAPABILITY_TILTY = 0x0100;
    static final int CAPABILITY_ROTATION = 0x0200;
    static final int CAPABILITY_PRESSURE = 0x0400;

    static final int kCGEventSourceStateHIDSystemState = 1;
    static final int kCGHIDEventTap = 0;

    /** tablet coordinate resolution */
    static final int TABLET_RESOLUTION = 0xffff;

    /** arbitrary ids, must be consistent between proximity and point events */
    static final int DEVICE_ID = 0x5a;
    static final int VENDOR_ID = 0x05ac; // apple
    static final int TABLET_ID = 0x1;
    static final int POINTER_ID = 0x1;
    static final long SERIAL = 0x1234_5678L;
    static final long UNIQUE_ID = 0x0a9e_1e9e_9c11L;

    /** CGPoint by value */
    public static class CGPoint extends Structure implements Structure.ByValue {
        public double x;
        public double y;

        public CGPoint() {
        }

        public CGPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        protected List<String> getFieldOrder() {
            return List.of("x", "y");
        }
    }

    /** minimal CoreGraphics binding */
    interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);

        Pointer CGEventSourceCreate(int stateID);
        Pointer CGEventCreate(Pointer source);
        Pointer CGEventCreateMouseEvent(Pointer source, int mouseType, CGPoint mouseCursorPosition, int mouseButton);
        void CGEventSetType(Pointer event, int type);
        void CGEventSetLocation(Pointer event, CGPoint location);
        void CGEventSetIntegerValueField(Pointer event, int field, long value);
        void CGEventSetDoubleValueField(Pointer event, int field, double value);
        void CGEventPost(int tap, Pointer event);
    }

    /** minimal CoreFoundation binding */
    interface CoreFoundation extends Library {
        CoreFoundation INSTANCE = Native.load("CoreFoundation", CoreFoundation.class);

        void CFRelease(Pointer cf);
    }

    private final CoreGraphics cg = CoreGraphics.INSTANCE;

    private final Pointer source;

    /** target area in global display coordinates (points) */
    private final Rectangle2D baseArea;

    /** effective area (aspect fitted) */
    private Rectangle2D area;

    private final boolean keepAspect = Boolean.parseBoolean(System.getProperty("vavi.apps.hub.tablet.keepAspect", "true"));

    private boolean inProximity;

    /** current pressed button, -1 when not pressed */
    private int pressed = -1;

    public TabletPoster() {
        source = cg.CGEventSourceCreate(kCGEventSourceStateHIDSystemState);
        baseArea = parseArea(System.getProperty("vavi.apps.hub.tablet.area"));
        area = baseArea;
logger.log(Level.DEBUG, "tablet area: " + area);
    }

    /** @param spec "x,y,w,h" or null */
    static Rectangle2D parseArea(String spec) {
        if (spec != null && !spec.isBlank()) {
            String[] v = spec.split(",");
            if (v.length == 4) {
                return new Rectangle2D.Double(Double.parseDouble(v[0].trim()), Double.parseDouble(v[1].trim()),
                        Double.parseDouble(v[2].trim()), Double.parseDouble(v[3].trim()));
            }
            logger.log(Level.WARNING, "illegal vavi.apps.hub.tablet.area: " + spec);
        }
        // main screen, awt coordinates (points, origin top-left of main screen) are the same as CG global ones
        Rectangle r = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        return new Rectangle2D.Double(r.x, r.y, r.width, r.height);
    }

    /** fits a w:h rectangle into the area, centered */
    static Rectangle2D fit(Rectangle2D area, double w, double h) {
        if (w <= 0 || h <= 0) return area;
        double scale = Math.min(area.getWidth() / w, area.getHeight() / h);
        double fw = w * scale;
        double fh = h * scale;
        return new Rectangle2D.Double(area.getX() + (area.getWidth() - fw) / 2, area.getY() + (area.getHeight() - fh) / 2, fw, fh);
    }

    /** normalized tablet position to screen position */
    static CGPoint map(Rectangle2D area, double nx, double ny) {
        nx = Math.max(0, Math.min(1, nx));
        ny = Math.max(0, Math.min(1, ny));
        return new CGPoint(area.getX() + nx * area.getWidth(), area.getY() + ny * area.getHeight());
    }

    /** entry point */
    public synchronized void post(TabletData d) {
        if (d == null || d.t == null) return;
        switch (d.t) {
            case "hello" -> {
                area = keepAspect ? fit(baseArea, d.w, d.h) : baseArea;
logger.log(Level.DEBUG, "tablet surface: " + d.w + "x" + d.h + ", area: " + area);
            }
            case "hover" -> {
                proximity(d, true);
                pointer(d, kCGEventMouseMoved, 0);
            }
            case "down" -> {
                proximity(d, true);
                pressed = d.b == 1 ? 1 : 0;
                pointer(d, pressed == 1 ? kCGEventRightMouseDown : kCGEventLeftMouseDown, pressed);
            }
            case "move" -> {
                if (pressed < 0) {
                    proximity(d, true);
                    pointer(d, kCGEventMouseMoved, 0);
                } else {
                    pointer(d, pressed == 1 ? kCGEventRightMouseDragged : kCGEventLeftMouseDragged, pressed);
                }
            }
            case "up" -> {
                if (pressed >= 0) {
                    d.p = 0;
                    pointer(d, pressed == 1 ? kCGEventRightMouseUp : kCGEventLeftMouseUp, pressed);
                    pressed = -1;
                }
            }
            case "leave" -> release(d);
            default -> logger.log(Level.DEBUG, "unknown type: " + d.t);
        }
    }

    /** ensures button up and proximity out, e.g. on disconnect */
    public synchronized void release(TabletData d) {
        if (d == null) {
            d = new TabletData();
            d.x = -1; // keep current location
        }
        if (pressed >= 0) {
            d.p = 0;
            pointer(d, pressed == 1 ? kCGEventRightMouseUp : kCGEventLeftMouseUp, pressed);
            pressed = -1;
        }
        proximity(d, false);
    }

    /** last posted location */
    private CGPoint last;

    private CGPoint location(TabletData d) {
        if (d.x < 0 && last != null) return last;
        last = map(area, d.x, d.y);
        return last;
    }

    private void pointer(TabletData d, int type, int button) {
        CGPoint pt = location(d);
        Pointer e = cg.CGEventCreateMouseEvent(source, type, pt, button);
        if (e == null) {
            logger.log(Level.WARNING, "CGEventCreateMouseEvent failed");
            return;
        }
        try {
            double p = Math.max(0, Math.min(1, d.p));
            cg.CGEventSetIntegerValueField(e, kCGMouseEventSubtype, kCGEventMouseSubtypeTabletPoint);
            cg.CGEventSetIntegerValueField(e, kCGMouseEventButtonNumber, button);
            if (type != kCGEventMouseMoved) {
                cg.CGEventSetIntegerValueField(e, kCGMouseEventClickState, 1);
            }
            cg.CGEventSetDoubleValueField(e, kCGMouseEventPressure, p);
            cg.CGEventSetIntegerValueField(e, kCGTabletEventPointX, Math.round(Math.max(0, Math.min(1, d.x)) * TABLET_RESOLUTION));
            cg.CGEventSetIntegerValueField(e, kCGTabletEventPointY, Math.round(Math.max(0, Math.min(1, d.y)) * TABLET_RESOLUTION));
            cg.CGEventSetIntegerValueField(e, kCGTabletEventPointButtons, pressed >= 0 && type != kCGEventLeftMouseUp && type != kCGEventRightMouseUp ? 1L << button : 0);
            cg.CGEventSetDoubleValueField(e, kCGTabletEventPointPressure, p);
            cg.CGEventSetDoubleValueField(e, kCGTabletEventTiltX, Math.max(-1, Math.min(1, d.tx)));
            cg.CGEventSetDoubleValueField(e, kCGTabletEventTiltY, Math.max(-1, Math.min(1, d.ty)));
            cg.CGEventSetDoubleValueField(e, kCGTabletEventRotation, d.r);
            cg.CGEventSetIntegerValueField(e, kCGTabletEventDeviceID, DEVICE_ID);
            cg.CGEventPost(kCGHIDEventTap, e);
        } finally {
            CoreFoundation.INSTANCE.CFRelease(e);
        }
    }

    private void proximity(TabletData d, boolean enter) {
        if (inProximity == enter) return;
        CGPoint pt = location(d);
        Pointer e = cg.CGEventCreateMouseEvent(source, kCGEventMouseMoved, pt, 0);
        if (e == null) {
            logger.log(Level.WARNING, "CGEventCreateMouseEvent failed");
            return;
        }
        try {
            cg.CGEventSetType(e, kCGEventTabletProximity);
            cg.CGEventSetIntegerValueField(e, kCGMouseEventSubtype, kCGEventMouseSubtypeTabletProximity);
            cg.CGEventSetIntegerValueField(e, kCGTabletProximityEventVendorID, VENDOR_ID);
            cg.CGEventSetIntegerValueField(e, kCGTabletProximityEventTabletID, TABLET_ID);
            cg.CGEventSetIntegerValueField(e, kCGTabletProximityEventPointerID, POINTER_ID);
            cg.CGEventSetIntegerValueField(e, kCGTabletProximityEventDeviceID, DEVICE_ID);
            cg.CGEventSetIntegerValueField(e, kCGTabletProximityEventSystemTabletID, TABLET_ID);
            cg.CGEventSetIntegerValueField(e, kCGTabletProximityEventVendorPointerType, 0x0802); // generic stylus
            cg.CGEventSetIntegerValueField(e, kCGTabletProximityEventVendorPointerSerialNumber, SERIAL);
            cg.CGEventSetIntegerValueField(e, kCGTabletProximityEventVendorUniqueID, UNIQUE_ID);
            cg.CGEventSetIntegerValueField(e, kCGTabletProximityEventCapabilityMask,
                    CAPABILITY_ABSX | CAPABILITY_ABSY | CAPABILITY_BUTTONS | CAPABILITY_TILTX | CAPABILITY_TILTY | CAPABILITY_ROTATION | CAPABILITY_PRESSURE);
            cg.CGEventSetIntegerValueField(e, kCGTabletProximityEventPointerType, NSPointingDeviceTypePen);
            cg.CGEventSetIntegerValueField(e, kCGTabletProximityEventEnterProximity, enter ? 1 : 0);
            cg.CGEventPost(kCGHIDEventTap, e);
            inProximity = enter;
logger.log(Level.TRACE, "proximity: " + enter);
        } finally {
            CoreFoundation.INSTANCE.CFRelease(e);
        }
    }
}
