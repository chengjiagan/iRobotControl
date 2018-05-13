package cn.edu.sjtu.watermelongan.irobotcontrol;

import android.view.InputDevice;
import android.view.MotionEvent;

class Gamepad {
    private MotionEvent mEvent;
    private InputDevice mDevice;

    final static int UP = 0;
    final static int LEFT = 1;
    final static int RIGHT = 2;
    final static int DOWN = 3;

    public Gamepad(MotionEvent mEvent) {
        this.mEvent = mEvent;
        this.mDevice = mEvent.getDevice();
    }

    public float getRadius() {
        float x = getCenteredAxis(MotionEvent.AXIS_X);
        float y = getCenteredAxis(MotionEvent.AXIS_Y);
        float r = (float) Math.sqrt(x*x + y*y);
        if (r > 1) {
            r = 1;
        }
        return r;
    }

    public float getAngle() {
        float x = Math.abs(getCenteredAxis(MotionEvent.AXIS_X));
        float y = Math.abs(getCenteredAxis(MotionEvent.AXIS_Y));
        float theta = (float) Math.atan2(y, x);
        return theta;
    }

    public float getCenteredAxis(int axis) {
        InputDevice.MotionRange range = mDevice.getMotionRange(axis);
        float value = mEvent.getAxisValue(axis);

        if (range != null) {
            float flat = range.getFlat();
            if (Math.abs(value) > flat) {
                return value;
            } else {
                return 0;
            }
        }

        return value;
    }

    public float getTrigger(int axis) {
        float trigger = 0;
        int historicSize = mEvent.getHistorySize();

        for (int i = 0; i < historicSize; i++) {
            trigger += mEvent.getHistoricalAxisValue(axis, i);
        }
        trigger += mEvent.getAxisValue(axis);

        return trigger;
    }

    public int getDpad() {
        int directionPressed = 5;
        float x_axis = mEvent.getAxisValue(MotionEvent.AXIS_HAT_X);
        float y_axis = mEvent.getAxisValue(MotionEvent.AXIS_HAT_Y);

        // Check if the AXIS_HAT_X value is -1 or 1, and set the D-pad
        // LEFT and RIGHT direction accordingly.
        if (Float.compare(x_axis, -1.0f) == 0) {
            directionPressed =  Gamepad.LEFT;
        } else if (Float.compare(x_axis, 1.0f) == 0) {
            directionPressed =  Gamepad.RIGHT;
        }
        // Check if the AXIS_HAT_Y value is -1 or 1, and set the D-pad
        // UP and DOWN direction accordingly.
        else if (Float.compare(y_axis, -1.0f) == 0) {
            directionPressed =  Gamepad.UP;
        } else if (Float.compare(y_axis, 1.0f) == 0) {
            directionPressed =  Gamepad.DOWN;
        }

        return directionPressed;
    }
}
