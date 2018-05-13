package cn.edu.sjtu.watermelongan.irobotcontrol;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

class BtRobotControl {

    private OutputStream mOutputStream;

    private int leftVelocity, rightVelocity, maxVelocity, accelerate;
    private boolean isStart = false,
                    isVacuum = true;

    private final String LogTag = "BtRobotControl";

    public BtRobotControl(OutputStream outputStream) {
        mOutputStream = outputStream;
        maxVelocity = 250;
        accelerate =10;
    }

    public void start() {
        int[] buffer = {0x80, 0x84};
        write(buffer);
        isStart = true;
    }

    public void stop() {
        int[] buffer = {0xAD};
        write(buffer);
        isStart = false;
    }

    public void setVelocity(int left, int right) {
        if (!isStart) {
            start();
        }

        if (left <= -maxVelocity || left >= maxVelocity
                || right <= -maxVelocity || left >= maxVelocity) {
            leftVelocity = left > 0 ? maxVelocity : -maxVelocity;
            rightVelocity = right > 0 ? maxVelocity : -maxVelocity;
            Log.e(LogTag, " -- velocity out of range");
        } else {
            leftVelocity = left;
            rightVelocity = right;
        }

        int[] buffer = {0x91,
                (leftVelocity >>> 8) & 0xFF,
                leftVelocity & 0xFF,
                (rightVelocity >>> 8) & 0xFF,
                rightVelocity & 0xFF};
        write(buffer);
    }

    public void startVacuum() {
        if (!isStart) {
            start();
        }

        int[] buffer = {0x8A, 0x06};
        write(buffer);
        isVacuum = true;
    }

    public void stopVacuum() {
        int[] buffer = {0x8A, 0x00};
        write(buffer);
        isVacuum = false;
    }

    public int getAccelerate() {
        return accelerate;
    }

    public int getLeftVelocity() {
        return leftVelocity;
    }

    public int getRightVelocity() {
        return rightVelocity;
    }

    public int getMaxVelocity() {
        return maxVelocity;
    }

    public void setMaxVelocity(int maxVelocity) {
        if (maxVelocity <= 500) {
            if (maxVelocity >= 0) {
                this.maxVelocity = maxVelocity;
            } else {
                this.maxVelocity = 0;
                Log.e(LogTag, " -- maxVelocity too low");
            }
        } else {
            this.maxVelocity = 500;
            Log.e(LogTag, " -- maxVelocity too high");
        }
    }

    public void turnRight(int vel) {
        int[] buffer = {
                0x89,
                (vel >>> 8) & 0xFF, vel & 0xFF,
                0xFF, 0xFF
        };
        write(buffer);
    }

    public void turnLeft(int vel) {
        int[] buffer = {
                0x89,
                (vel >>> 8) & 0xFF, vel & 0xFF,
                0x00, 0x01
        };
        write(buffer);
    }

    public void goStraight(int vel) {
        int[] buffer = {
                0x89,
                (vel >>> 8) & 0xFF, vel & 0xFF,
                0x80, 0x00
        };
        write(buffer);
    }

    public void turnRadius(int vel, int radius) {
        int buffer[] = {
                0x89,
                (vel >>> 8) & 0xFF, vel & 0xFF,
                (radius >>> 8) & 0xFF, radius & 0xFF
        };
        write(buffer);
    }

    private void write(int[] buffer) {
        try {
            for (int operation : buffer) {
                mOutputStream.write(operation);
            }
            Log.i(LogTag, " -- write " + Arrays.toString(buffer));
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LogTag, " -- fail to write");
        }
    }
}
