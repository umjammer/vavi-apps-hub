/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.listener;

import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import net.java.games.input.Event;
import vavi.games.input.listener.GamepadInputEventListener.AppInfo;
import vavi.games.input.listener.GamepadInputEventListener.Context;
import vavi.games.input.robot.Key;
import vavi.games.input.robot.RococaRobot;
import vavi.util.Debug;
import vavi.util.event.GenericEvent;

import static org.rococoa.carbon.CarbonCoreLibrary.kVK_ANSI_0;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_ANSI_2;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_ANSI_5;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_ANSI_A;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_ANSI_D;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_ANSI_E;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_ANSI_F;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_ANSI_Q;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_ANSI_S;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_ANSI_W;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_Control;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_Escape;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_F3;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_F5;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_F8;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_Option;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_Shift;
import static org.rococoa.carbon.CarbonCoreLibrary.kVK_Space;
import static org.rococoa.cocoa.coregraphics.CoreGraphicsLibrary.kCGMouseButtonLeft;
import static org.rococoa.cocoa.coregraphics.CoreGraphicsLibrary.kCGMouseButtonRight;
import static vavi.games.input.helper.JavaVMAppInfo.getPidByMainClassName;


/**
 * MinecraftListener.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-03-20 nsano initial version <br>
 */
public class MinecraftListener extends GamepadAdapter {

    private static final String bundleId = "com.mojang.Minecraft";

    /** minecraft launchers descriptor#dusplayName */
    private static final String[] mcLaunchers;

    static {
        try {
            Properties props = new Properties();
            props.load(MinecraftListener.class.getResourceAsStream("/minecraft.properties"));
            mcLaunchers = props.stringPropertyNames().toArray(String[]::new);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private long prevForBounds;

    @Override
    public boolean match(AppInfo a) {
        try {
            if (a.pid() == getPidByMainClassName(mcLaunchers)) {
                if (System.currentTimeMillis() - prevForBounds > 20 * 1000) {
                    prevForBounds = System.currentTimeMillis();
                    Executors.newSingleThreadScheduledExecutor().submit(() -> {
                        Rectangle b = a.bounds();
                        if (b != null) {
                            Rectangle r = bounds.get();
                            if (r == null) {
                                bounds.set(b);
                            } else {
                                r.setBounds(b);
                            }
//Debug.println("minecraft window found: " + r);
//                        } else {
//Debug.println("no minecraft window found.");
                        }
                    });
                }
                return true;
            }
        } catch (NoSuchElementException e) {
//Debug.println(e.getMessage());
        }
        return false;
    }

    // ----

    private final RococaRobot robot = new RococaRobot();

    private long prev = System.currentTimeMillis();

    private final DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
    private static final float deadZone = 6;
    private static final float threshold = 30;
    private static final int intervalWheel = 6;
    private static final float d = 1600;

    private Point point;
    private int dx, dy;

    private boolean moved;

    private final AtomicReference<Rectangle> bounds = new AtomicReference<>();

    /**
     * @after {@link #point}
     */
    private void normalizePoint() {
        Rectangle b = bounds.get();
        if (b != null) {
            if (point.x < b.x) point.x = b.x;
            if (point.x > b.x + b.width) point.x = b.x + b.width - 1;
            if (point.y < b.y) point.y = b.y;
            if (point.y > b.y + b.height) point.y = b.y + b.height - 1;
        } else {
            if (point.x < 0) point.x = 0;
            if (point.x > dm.getWidth()) point.x = dm.getWidth() - 1;
            if (point.y < 0) point.y = 0;
            if (point.y > dm.getHeight()) point.y = dm.getHeight() - 1;
        }
    }

    class RobotKey extends Key {
        RobotKey(int code) {
            super(code, robot::keyPress, robot::keyRelease);
        }
    }

    class RobotKey2 extends Key {
        RobotKey2(int code) {
            super(code, robot::keyPressRaw, robot::keyReleaseRaw);
        }
    }

    class RobotMouseKey extends Key {
        RobotMouseKey(int code) {
            super(code, robot::mousePress, robot::mouseRelease);
        }
    }

    final Key key_D = new RobotKey(kVK_ANSI_D);
    final Key key_A = new RobotKey(kVK_ANSI_A);
    final Key key_S = new RobotKey(kVK_ANSI_S);
    final Key key_W = new RobotKey(kVK_ANSI_W);
    final Key key_F = new RobotKey(kVK_ANSI_F);
    final Key key_F8 = new RobotKey(kVK_F8);
    final Key key_0 = new RobotKey(kVK_ANSI_0);
    final Key key_SPACE = new RobotKey(kVK_Space);
    final Key key_2 = new RobotKey(kVK_ANSI_2);
    final Key key_5 = new RobotKey(kVK_ANSI_5);
    final Key mouseKey_0 = new RobotMouseKey(kCGMouseButtonLeft);
    final Key mouseKey_1 = new RobotMouseKey(kCGMouseButtonRight);
    final Key key_CONTROL = new RobotKey2(kVK_Control);
    final Key key_SHIFT = new RobotKey2(kVK_Shift);
    final Key key_ESCAPE = new RobotKey(kVK_Escape);
    final Key key_Q = new RobotKey(kVK_ANSI_Q);
    final Key key_OPTION = new RobotKey2(kVK_Option);
    final Key key_F5 = new RobotKey(kVK_F5);
    final Key key_F3 = new RobotKey(kVK_F3);
    final Key key_E = new RobotKey(kVK_ANSI_E);

    private Context context;

    @Override
    public void init(Context context) {
        this.context = context;
    }

    @Override
    public void active() {
        context.fireEventHappened(new GenericEvent(this, "gamepad.listener.changed", bundleId));
    }

    @Override
    public void deactive() {
        // TODO redundant
        context.fireEventHappened(new GenericEvent(this, "gamepad.listener.changed", (Object) null));
    }

    @Override
    public void before() {
        moved = false;
        point = MouseInfo.getPointerInfo().getLocation();
    }

    /** L3 X Axis ... - 'A' 0x00, + 'D' 0x02 */
    @Override
    public void onX(Event e) {
        float v = e.getValue() - 128;
        float a = Math.abs(v);

        if (a > threshold) {
            if (Math.signum(v) > 0) {
                key_D.press();
Debug.println(Level.FINER, "L3 axis x+ ... D");
            } else {
                key_A.press();
Debug.println(Level.FINER, "L3 axis x- ... A");
            }
        } else {
            key_D.release();
            key_A.release();
        }
    }

    /** L3 Y Axis ... - 'S' 0x01, + 'W' 0x0d */
    @Override
    public void onY(Event e) {
        float v = e.getValue() - 128;
        float a = Math.abs(v);

        if (a > threshold) {
            if (Math.signum(v) > 0) {
                key_S.press();
Debug.println(Level.FINER, "L3 axis y- ... S");
            } else {
                key_W.press();
Debug.println(Level.FINER, "L3 axis y+ ... W");
            }
        } else {
            key_S.release();
            key_W.release();
        }
    }

    /** R3 X Axis ... Mouse X Axis  */
    @Override
    public void onZ(Event e) {
        float v = e.getValue() - 128;
        float a = Math.abs(v);
        if (a > deadZone) {
            v = v * a / d;
            dx = (int) v;
            point.x += dx;
            moved = true;
Debug.println(Level.FINER, "R3 axis x ... " + (v > 0 ? "+" : "") + v + ", " + a);
        }
    }

    /** R3 Y Axis ... Mouse Y Axis */
    @Override
    public void onRZ(Event e) {
        float v = e.getValue() - 128;
        float a = Math.abs(v);
        if (a > deadZone) {
            v = v * a / d;
            dy = (int) -v;
            point.y += dy;
            moved = true;
Debug.println(Level.FINER, "R3 axis y ... " + (v > 0 ? "+" : "") + v + ", " + a);
        }
    }

    /**
     * 0: ↑ ... 'F' 0x03
     * 1: ↗
     * 2: → ... Mouse Wheel 1 +
     * 3: ↘
     * 4: ↓ ... F8 0x68
     * 5: ↙
     * 6: ← ... Mouse Wheel 1 -
     * 7: ↖
     */
    @Override
    public void onHatSwitch(Event e) {
        int v = (int) e.getValue();
        switch (v) {
            case 0 -> {
                key_F.press();
Debug.println(Level.FINER, "↑");
            }
            case 2 -> {
                if (System.currentTimeMillis() - prev > intervalWheel) {
                    robot.mouseWheel(-1);
Debug.println(Level.FINER, "→ ... mouse wheel +");
                    prev = System.currentTimeMillis();
                }
            }
            case 4 -> {
                key_F8.press();
Debug.println(Level.FINER, "↓");
            }
            case 6 -> {
                if (System.currentTimeMillis() - prev > intervalWheel) {
                    robot.mouseWheel(+1);
Debug.println(Level.FINER, "← ... mouse wheel +");
                    prev = System.currentTimeMillis();
                }
            }
            default -> {
                key_F.release();
                key_F8.release();
            }
        }
    }

    /** □ ... '0' 0x52 */
    @Override
    public void onButton1(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_0.press();
Debug.println(Level.FINER, "□ ... 0");
        } else {
            key_0.release();
        }
    }

    /** ✗ ... SPACE 0x31 */
    @Override
    public void onButton2(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_SPACE.press();
Debug.println(Level.FINER, "✗ ... SPACE");
        } else {
            key_SPACE.release();
        }
    }

    /** ◯ ... '2' 0x54 */
    @Override
    public void onButton3(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_2.press();
Debug.println(Level.FINER, "◯ ... 2");
        } else {
            key_2.release();
        }
    }

    /** △ ... '5' 0x57 */
    @Override
    public void onButton4(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_5.press();
Debug.println(Level.FINER, "△ ... 5");
        } else {
            key_5.release();
        }
    }

    /** L1 ... Mouse Click 0 */
    @Override
    public void onButton5(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            mouseKey_0.press();
Debug.println(Level.FINER, "L1 ... Mouse Click 0");
        } else {
            mouseKey_0.release();
        }
    }

    /** R1 ... Mouse Click 1 */
    @Override
    public void onButton6(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            mouseKey_1.press();
Debug.println(Level.FINER, "R1 ... Mouse Click 1");
        } else {
            mouseKey_1.release();
        }
    }

    /** L2  ... Control Left 0x3B */
    @Override
    public void onButton7(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_CONTROL.press();
Debug.println(Level.FINER, "L2 ... Control Left");
        } else {
            key_CONTROL.release();
        }
    }

    /** R2 ... Shift Left 0x38 */
    @Override
    public void onButton8(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_SHIFT.press();
Debug.println(Level.FINER, "R2 ... Shift Left");
        } else {
            key_SHIFT.release();
        }
    }

    /** SHARE ... ESC 0x35 */
    @Override
    public void onButton9(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_ESCAPE.press();
Debug.println(Level.FINER, "SHARE ... ESC");
        } else {
            key_ESCAPE.release();
        }
    }

    /** OPTIONS ... 'Q' 0x0c */
    @Override
    public void onButton10(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_Q.press();
Debug.println(Level.FINER, "OPTIONS ... Q");
        } else {
            key_Q.release();
        }
    }

    /** L3 ... Option Left 0x3A */
    @Override
    public void onButton11(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_OPTION.press();
Debug.println(Level.FINER, "L3 Button ... Option Left");
        } else {
            key_OPTION.release();
        }
    }

    /** R3 ... F5 0x60 */
    @Override
    public void onButton12(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_F5.press();
Debug.println(Level.FINER, "R3 Button ... F5");
        } else {
            key_F5.release();
        }
    }

    /** PS ... F3 0x63 */
    @Override
    public void onButton13(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_F3.press();
Debug.println(Level.FINER, "PS ... F3");
        } else {
            key_F3.release();
        }
    }

    /** PAD ... E 0x0e */
    @Override
    public void onButton14(Event e) {
        boolean v = e.getValue() != 0;

        if (v) {
            key_E.press();
Debug.println(Level.FINER, "PAD: ... E");
        } else {
            key_E.release();
        }
    }

    @Override
    public void after() {
        if (moved) {
            robot.mouseMoveOnlyAccel(dx, dy);
            normalizePoint();
            robot.mouseMoveOnlyLocation(point.x, point.y);
        }
    }
}
