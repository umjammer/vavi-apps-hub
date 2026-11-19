/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub.ws;

/**
 * A pen sample sent by the tablet page (static/tablet.html).
 * <p>
 * wire format (json):
 * <pre>
 * {"t":"down|move|up|hover|leave|hello", "x":0..1, "y":0..1, "p":0..1, "tx":-1..1, "ty":-1..1, "r":deg, "b":0|1, "w":px, "h":px}
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-11 nsano initial version <br>
 */
public class TabletData {

    /** event type: hello, down, move, up, hover, leave */
    public String t;
    /** normalized x on the tablet surface [0, 1] */
    public double x;
    /** normalized y on the tablet surface [0, 1] */
    public double y;
    /** pressure [0, 1] */
    public double p;
    /** tilt x [-1, 1] (= PointerEvent#tiltX / 90) */
    public double tx;
    /** tilt y [-1, 1] (= PointerEvent#tiltY / 90) */
    public double ty;
    /** rotation (twist) in degrees */
    public double r;
    /** button, 0: left (pen tip), 1: right */
    public int b;
    /** tablet surface width in css pixel (hello only) */
    public double w;
    /** tablet surface height in css pixel (hello only) */
    public double h;

    @Override
    public String toString() {
        return "TabletData{" + t + ", " + x + ", " + y + ", p=" + p + ", tilt=" + tx + "," + ty + ", b=" + b + '}';
    }
}
