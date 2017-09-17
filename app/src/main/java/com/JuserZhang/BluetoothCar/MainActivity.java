package com.JuserZhang.BluetoothCar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.JuserZhang.BluetoothCar.util.LogUtil;
import com.JuserZhang.BluetoothCar.util.bluetooth.BluetoothUtil;
import com.JuserZhang.BluetoothCar.util.bluetooth.OnDataRecivedlistener;
import com.JuserZhang.BluetoothCar.util.bluetooth.OnLoglistener;
import com.JuserZhang.BluetoothCar.widget.Direction;
import com.JuserZhang.BluetoothCar.widget.Rudder;
import com.JuserZhang.BluetoothCar.widget.Rudder.RudderListener;

import java.io.OutputStream;

public class MainActivity extends Activity implements RudderListener, SensorEventListener {
    // 调试用
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean debug = true;

    // 蓝牙UUID
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    // 协议常量
    private static byte[] data = new byte[]{0x00, 0x0D, 0x0A};

    // 提示内容
    private TextView mTextView = null;
    private TextView mStateTv = null;
    private TextView mLogTv = null;
    private TextView mDataReceiedTv = null;
    private TextView mSpeedvalueTx = null;
    // 虚拟摇杆
    private Rudder mRudder = null;

    // 加载动画
    private ImageView mWheelView = null;
    private Animation mAnimation = null;

    // 蓝牙API
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothSocket mSocket = null;
    private OutputStream mOutS = null;

    // 加速(重力)传感器API
    private SensorManager mSensorManager = null;
    private Sensor mSensor = null;

    // 加速(重力)传感器开关
    private ToggleButton mToggle = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置无标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        initUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initDevice();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 初始化传感器
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onPause() {
        // 注销传感器
        mSensorManager.unregisterListener(this);

        super.onPause();
    }

    @Override
    protected void onStop() {
        BluetoothUtil.getInstance().close();
        super.onStop();
    }

    /**
     * 初始化UI
     */
    int accelerate_value = 0;

    private void initUi() {
        mTextView = (TextView) findViewById(R.id.text_view);
        mStateTv = (TextView) findViewById(R.id.state_tv);
        mLogTv = (TextView) findViewById(R.id.log_tv);
        mDataReceiedTv = (TextView) findViewById(R.id.data_receied_tv);
        mRudder = (Rudder) findViewById(R.id.rudder);
        mWheelView = (ImageView) findViewById(R.id.wheel_view);
        mSpeedvalueTx = (TextView) findViewById(R.id.speedvalue);
        final Button accelerate_btn = (Button) findViewById(R.id.accelerate_btn);
        final Button decelerate_btn = (Button) findViewById(R.id.decelerate_btn);
        decelerate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data[0] = 0x06;
                BluetoothUtil.getInstance().sendData(data);
                accelerate_value--;
                if (accelerate_value > 0) {
                    mSpeedvalueTx.setText("目前速度:默认速度+" + accelerate_value);
                } else if (accelerate_value == 0) {
                    mSpeedvalueTx.setText("目前速度:默认速度");
                } else {
                    mSpeedvalueTx.setText("目前速度:默认速度" + accelerate_value);
                }
            }
        });
        accelerate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data[0] = 0x05;
                accelerate_value++;
                BluetoothUtil.getInstance().sendData(data);
                if (accelerate_value > 0) {
                    mSpeedvalueTx.setText("目前速度:默认速度+" + accelerate_value);
                } else if (accelerate_value == 0) {
                    mSpeedvalueTx.setText("目前速度:默认速度");
                } else {
                    mSpeedvalueTx.setText("目前速度:默认速度" + accelerate_value);
                }
            }
        });

        // 设置监听器
        mRudder.setOnRudderListener(this);

        // 加载动画
        mAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);

        // 设置匀速旋转速率
        mAnimation.setInterpolator(new LinearInterpolator());

        mToggle = (ToggleButton) findViewById(R.id.toggle);

        mToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 注册传感器
                    mSensorManager.registerListener(MainActivity.this, mSensor, SensorManager.SENSOR_DELAY_GAME);
                } else {
                    // 注销传感器
                    mSensorManager.unregisterListener(MainActivity.this);
                    data[0] = 0x00;
                    BluetoothUtil.getInstance().sendData(data);
                }
            }
        });
        BluetoothUtil.getInstance().setOnLoglistener(new OnLoglistener() {
            @Override
            public void OnAddLog(final Object o) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLogTv.setText("正在发送:" + o.toString());
                    }
                });
            }
        });
        registerBluetoothDeviceListioner();
        BluetoothUtil.getInstance().readDataFromServer(new OnDataRecivedlistener() {
            @Override
            public void onDataRecived(final Object o) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDataReceiedTv.setText("收到数据:" + o.toString());
                    }
                });
            }
        });

    }

    /**
     * 初始化设备
     */
    private void initDevice() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mBluetoothDevice = bundle.getParcelable("device");
            if (mBluetoothDevice != null) {
                String name = mBluetoothDevice.getName();
                mStateTv.setText(name + ":已连接");
            }
        }
    }


    @Override
    public void onSteeringWheelChanged(int action, Direction direction) {
        if (action == Rudder.ACTION_RUDDER) {
            switch (direction) {
                case LEFT_DOWN_DIR:
                    LogUtil.i(debug, TAG, "[1] --> left down...");
                    mTextView.setText("左后方");
                    data[0] = 0x07;
                    break;

                case LEFT_DIR:
                    LogUtil.i(debug, TAG, "[2] --> turn left...");
                    mTextView.setText("左转");
                    data[0] = 0x03;
                    break;

                case LEFT_UP_DIR:
                    LogUtil.i(debug, TAG, "[3] --> left up...");
                    mTextView.setText("左前方");
                    data[0] = 0x05;
                    break;

                case UP_DIR:
                    LogUtil.i(debug, TAG, "[4] --> go forward...");
                    mTextView.setText("前进");
                    data[0] = 0x01;
                    break;

                case RIGHT_UP_DIR:
                    LogUtil.i(debug, TAG, "[5] --> right up...");
                    mTextView.setText("右前方");
                    data[0] = 0x06;
                    break;

                case RIGHT_DIR:
                    LogUtil.i(debug, TAG, "[6] --> turn right...");
                    mTextView.setText("右转");
                    data[0] = 0x04;
                    break;

                case RIGHT_DOWN_DIR:
                    LogUtil.i(debug, TAG, "[7] --> right down...");
                    mTextView.setText("右后方");
                    data[0] = 0x08;
                    break;

                case DOWN_DIR:
                    LogUtil.i(debug, TAG, "[8] --> go backward...");
                    mTextView.setText("后退");
                    data[0] = 0x02;
                    break;

                default:
                    break;
            }
            BluetoothUtil.getInstance().sendData(data);

        } else if (action == Rudder.ACTION_STOPPED) {
            LogUtil.i(debug, TAG, "[9] --> keep stopped..");
            mTextView.setText("暂停");
            data[0] = 0x00;
            BluetoothUtil.getInstance().sendData(data);
        }
    }

    @Override
    public void onAnimated(boolean isAnim) {
        if (isAnim) {
            mWheelView.startAnimation(mAnimation);
        } else {
            mWheelView.clearAnimation();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        LogUtil.i(debug, TAG, "【MainActivity.onSensorChanged()】【start】");
        if (event.sensor == null) {
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float gx = event.values[0];
            float gy = event.values[1];
            float gz = event.values[2];
            if (gx < -3) {
                LogUtil.i(debug, TAG, "[10] --> go forward...");
                mTextView.setText("重力感应:前进");
                data[0] = 0x01;
                BluetoothUtil.getInstance().sendData(data);
            } else if (gx > 6) {
                LogUtil.i(debug, TAG, "[11] --> go backward...");
                mTextView.setText("重力感应:后退");
                data[0] = 0x02;
                BluetoothUtil.getInstance().sendData(data);
            } else if (gy < -4) {
                LogUtil.i(debug, TAG, "[12] --> turn left...");
                mTextView.setText("重力感应:左转");
                data[0] = 0x03;
                BluetoothUtil.getInstance().sendData(data);
            } else if (gy > 4) {
                LogUtil.i(debug, TAG, "[13] --> turn right...");
                mTextView.setText("重力感应:右转");
                data[0] = 0x04;
                BluetoothUtil.getInstance().sendData(data);
            } else {
                LogUtil.i(debug, TAG, "[14] --> gx:" + gx + "------gy:" + gy + "------gz:" + gz);
                mTextView.setText("重力感应:暂停");
                data[0] = 0x00;
                BluetoothUtil.getInstance().sendData(data);
                onAnimated(false);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        LogUtil.i(debug, TAG, "【MainActivity.onAccuracyChanged()】【start】");
    }

    public void registerBluetoothDeviceListioner() {
        //注册广播接收器(监听蓝牙状态的改变)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
//      filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);//蓝牙扫描状态(SCAN_MODE)发生改变
//      filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); //指明一个远程设备的连接状态的改变。比如，当一个设备已经被匹配。
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);//指明一个与远程设备建立的低级别（ACL）连接。
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);//指明一个来自于远程设备的低级别（ACL）连接的断开
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);//指明一个为远程设备提出的低级别（ACL）的断开连接请求，并即将断开连接。
//      filter.addAction(BluetoothDevice.ACTION_FOUND);//发现远程设备
//      filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);//本地蓝牙适配器已经开始对远程设备的搜寻过程。
        this.registerReceiver(BluetoothReciever, filter); // 不要忘了之后解除绑定
    }

    //蓝牙状态监听
    private BroadcastReceiver BluetoothReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("==========此时蓝牙的状态是====11====" + intent.getAction());
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                System.out.println("==========此时蓝牙的状态是====22====" + btState);
                //打印蓝牙的状态
                printBTState(btState);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                String name = mBluetoothDevice.getName();
                mStateTv.setText(name + ":已断开");
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(intent.getAction())) {
                String name = mBluetoothDevice.getName();
                mStateTv.setText(name + ":即将断开");
            }
        }
    };

    //解除注册
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(BluetoothReciever);
    }

    //打印蓝牙的状态
    private void printBTState(int btState) {
        String name = mBluetoothDevice.getName();
        switch (btState) {
            case BluetoothAdapter.STATE_OFF:
                mStateTv.setText(name + ":已关闭");
                System.out.println("============蓝牙状态:已关闭===========" + btState);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                System.out.println("========蓝牙状态:正在关闭==============" + btState);
                mStateTv.setText(name + ":正在关闭");
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                System.out.println("=====蓝牙状态:正在打开======" + btState);//当蓝牙打开后自动连接设备
                mStateTv.setText(name + ":正在打开");
                break;
            case BluetoothAdapter.STATE_ON:
                System.out.println("=========蓝牙状态:已打开=========" + btState);
                mStateTv.setText(name + ":已打开");
                break;
            default:
                break;
        }
    }
}
