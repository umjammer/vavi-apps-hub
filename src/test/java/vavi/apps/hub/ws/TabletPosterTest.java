/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub.ws;

import java.awt.geom.Rectangle2D;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * TabletPosterTest. (no events are posted)
 */
class TabletPosterTest {

    @Test
    void testParseArea() {
        Rectangle2D r = TabletPoster.parseArea("10, 20, 300, 400");
        assertEquals(new Rectangle2D.Double(10, 20, 300, 400), r);
    }

    @Test
    void testFit() {
        Rectangle2D screen = new Rectangle2D.Double(0, 0, 1600, 1000);
        // ipad mini landscape 1133x744 css px
        Rectangle2D r = TabletPoster.fit(screen, 1133, 744);
        assertEquals(1000, r.getHeight(), 1e-6);
        assertEquals(1133 * 1000 / 744d, r.getWidth(), 1e-6);
        assertEquals((1600 - r.getWidth()) / 2, r.getX(), 1e-6);
        assertEquals(0, r.getY(), 1e-6);
        // invalid surface size keeps area
        assertEquals(screen, TabletPoster.fit(screen, 0, 0));
    }

    @Test
    void testMap() {
        Rectangle2D area = new Rectangle2D.Double(100, 50, 800, 600);
        TabletPoster.CGPoint p = TabletPoster.map(area, 0.5, 0.25);
        assertEquals(500, p.x, 1e-6);
        assertEquals(200, p.y, 1e-6);
        // clamped
        p = TabletPoster.map(area, 1.5, -0.2);
        assertEquals(900, p.x, 1e-6);
        assertEquals(50, p.y, 1e-6);
    }

    @Test
    void testDecode() {
        TabletData d = new Gson().fromJson("{\"t\":\"move\",\"x\":0.1,\"y\":0.2,\"p\":0.75,\"tx\":-0.5,\"ty\":0.25,\"r\":0,\"b\":0}", TabletData.class);
        assertEquals("move", d.t);
        assertEquals(0.75, d.p, 1e-9);
        assertEquals(-0.5, d.tx, 1e-9);
    }
}
