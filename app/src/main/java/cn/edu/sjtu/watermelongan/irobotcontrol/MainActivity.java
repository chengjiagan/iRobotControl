package cn.edu.sjtu.watermelongan.irobotcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private TextView textView;

    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private OutputStream outputStream;
    private BroadcastReceiver mBtConnectReceiver;
    private BroadcastReceiver mBtDisconnectReceiver;
    private boolean isConnect = false;
    private boolean isDpad = true;
    private boolean isVaccum = false;

    private BtRobotControl mRobot;

    private final String LogTag = "MainActivity";
    private final String address = "98:D3:33:80:D0:18";
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(LogTag, " -- MainActivity created");

        textView = findViewById(R.id.textView);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter  == null) {
            Log.e(LogTag, " -- Bluetooth unavailable");
            finish();
        }

        if (!mBtAdapter.isEnabled()) {
            Intent openBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(openBtIntent);
        }

        mDevice = mBtAdapter.getRemoteDevice(address);

        mBtConnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(LogTag, " -- connect");
                textView.setText(R.string.text_connect);
                Toast.makeText(MainActivity.this, R.string.toast_connect, Toast.LENGTH_LONG).show();
            }
        };
        IntentFilter mBtConnectFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(mBtConnectReceiver, mBtConnectFilter);

        mBtDisconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(LogTag, " -- disconnect");
                textView.setText(R.string.text_try_connect);
                isConnect = false;
                Toast.makeText(MainActivity.this, R.string.toast_disconnect, Toast.LENGTH_LONG).show();
                new TryConnect().start();
            }
        };
        IntentFilter mBtDisconnectFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mBtDisconnectReceiver, mBtDisconnectFilter);

        new TryConnect().run();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(LogTag, " -- MainActivity destroyed");

        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        unregisterReceiver(mBtConnectReceiver);
        unregisterReceiver(mBtDisconnectReceiver);
    }

    public void switchControl(View view) {
        isDpad = !isDpad;
        if(isDpad) {
            ((Button)view).setText(R.string.button_joystick);
        } else {
            ((Button)view).setText(R.string.button_dpad);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                && event.getAction() == MotionEvent.ACTION_MOVE) {
            Gamepad gamepad = new Gamepad(event);
            controlAccelerate(gamepad);

            if (isDpad) {
                controlMovement(gamepad);
            } else {
                controlVelocity(gamepad);
            }

            return true;
        }

        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BUTTON_A:
                    mRobot.start();
                    break;
                case KeyEvent.KEYCODE_BUTTON_B:
                    mRobot.stop();
                    break;
                case KeyEvent.KEYCODE_BUTTON_X:
                    if (isVaccum) {
                        mRobot.stopVacuum();
                    } else {
                        mRobot.startVacuum();
                    }
                    isVaccum = !isVaccum;
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    mRobot.goStraight(0);
                    break;
                default:
                    super.onKeyDown(keyCode, event);
                    break;
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    private void controlVelocity(Gamepad gamepad) {
        float x = gamepad.getCenteredAxis(MotionEvent.AXIS_X);
        float y = gamepad.getCenteredAxis(MotionEvent.AXIS_Y);
        float r = gamepad.getRadius();
        float theta = gamepad.getAngle();

        int max = y > 0? -mRobot.getMaxVelocity():mRobot.getMaxVelocity();
        int velocity = (int)r*max;

        if (Float.compare(y, 1.0f) == 0) {
            mRobot.goStraight(velocity);
            Log.i(LogTag, " -- go backward in " + Integer.toString(velocity));
            return;
        } else if (Float.compare(y, -1.0f) == 0) {
            mRobot.goStraight(velocity);
            Log.i(LogTag, " -- go ahead in " + Integer.toString(velocity));
            return;
        }

        if (Float.compare(x, 1.0f) == 0) {
            mRobot.turnRight(velocity);
            Log.i(LogTag, " -- turn right in "+ Integer.toString(velocity));
            return;
        } else if (Float.compare(x, -1.0f) == 0) {
            mRobot.turnLeft(velocity);
            Log.i(LogTag, " -- turn left in "+ Integer.toString(velocity));
            return;
        }

        int radius = (int)(theta / (Math.PI / 2) * 2000);
        if (x > 0) {
            radius = -radius;
        }
        mRobot.turnRadius(velocity, radius);
        Log.i(LogTag,
                " -- velocity = " + Integer.toString(velocity) + "; radius = " + Integer.toString(radius));
    }

    private void controlMovement(Gamepad gamepad) {
        int direction = gamepad.getDpad();
        switch (direction) {
            case Gamepad.UP:
                mRobot.goStraight(mRobot.getMaxVelocity());
                Log.i(LogTag, " -- go forward");
                break;
            case Gamepad.DOWN:
                mRobot.goStraight(-mRobot.getMaxVelocity());
                Log.i(LogTag, " -- go backward");
                break;
            case Gamepad.LEFT:
                mRobot.turnLeft(mRobot.getMaxVelocity());
                Log.i(LogTag, " -- turn left");
                break;
            case Gamepad.RIGHT:
                mRobot.turnRight(mRobot.getMaxVelocity());
                Log.i(LogTag, " -- turn right");
                break;
            default:
                Log.i(LogTag, " -- others");
        }
    }

    private void controlAccelerate(Gamepad gamepad) {
        float accelerateRatio = gamepad.getTrigger(MotionEvent.AXIS_RTRIGGER)
                - gamepad.getTrigger(MotionEvent.AXIS_LTRIGGER);
        int accelerate = (int)(mRobot.getAccelerate() * accelerateRatio);

        if(isConnect) {
            Log.i(LogTag, " -- acclerate = " + Integer.toString(accelerate) + "; maxVelocity = " + Integer.toString(mRobot.getMaxVelocity()));
            mRobot.setMaxVelocity(mRobot.getMaxVelocity() + accelerate);
            textView.setText(Integer.toString(mRobot.getMaxVelocity()) + "mm/s");
        }
    }

    private class TryConnect extends Thread {

        private final String LogTag = "TryConnect";

        @Override
        public void run() {
            Log.i(LogTag, " -- start connect");

            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LogTag, " -- fail to create socket");
            }


            while(!isConnect) {
                try {
                    if (!mSocket.isConnected()) {
                        mSocket.connect();
                        isConnect = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(LogTag, " -- fail to connect socket");
                }
            }



            try {
                outputStream = mSocket.getOutputStream();
                mRobot = new BtRobotControl(outputStream);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LogTag, " -- fail to create outputStream");
            }

            Log.i(LogTag, " -- connect to device successfully, end TryConnect thread.");
        }
    }
}
