package com.JuserZhang.BluetoothCar.util.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.JuserZhang.BluetoothCar.util.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 作者：deqing on 2017/9/17 18:22
 * 邮箱：18612205027@163.com
 */
public class BluetoothUtil {
    private static final String TAG = BluetoothUtil.class.getSimpleName();
    private static final boolean debug = true;

    private static volatile BluetoothUtil mBluetoothUtil;
    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    private OutputStream mOutS = null;
    private Activity mActivity;

    public BluetoothAdapter getmBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public void startScanDevice() {
        mBluetoothAdapter.startDiscovery();
    }

    public void stopScanDevice() {
        mBluetoothAdapter.cancelDiscovery();
    }

    private BluetoothUtil() {
        // 初始化本地蓝牙设备
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static BluetoothUtil getInstance() {
        if (mBluetoothUtil == null) {
            synchronized (BluetoothUtil.class) {
                if (mBluetoothUtil == null) {
                    mBluetoothUtil = new BluetoothUtil();
                }
            }
        }
        return mBluetoothUtil;
    }


    /**
     * 建立连接
     */
    public void connect(final BluetoothDevice btDev, final onDeviceConnectlistener onDeviceConnectlistener) {
        LogUtil.i(debug, TAG, "【BluetoothUtil.connect()】【btDev.getName()=" + btDev.getName() + "】");
        new Thread() {
            @Override
            public void run() {
                super.run();
                // 转化格式
                UUID uuid = UUID.fromString(SPP_UUID);
                try {
                    // 发起远程服务连接
                    mSocket = btDev.createRfcommSocketToServiceRecord(uuid);
                } catch (IOException e) {
                    if (mSocket != null) {
                        try {
                            mSocket.close();
                        } catch (IOException e1) {
                            LogUtil.e(debug, TAG, e.getMessage());
                        }
                    }
                    onDeviceConnectlistener.OnDeviceConnectedFailed();
                }
                try {
                    // 连接
                    mSocket.connect();
                } catch (IOException e) {
                    if (mSocket != null) {
                        try {
                            mSocket.close();
                        } catch (IOException e1) {
                            LogUtil.e(debug, TAG, e.getMessage());
                            LogUtil.e(debug, TAG, e1.getMessage());
                        }
                    }
                    onDeviceConnectlistener.OnDeviceConnectedFailed();
                }
                try {
                    // 获取输出流
                    mOutS = mSocket.getOutputStream();
                } catch (IOException e) {
                    if (mOutS != null) {
                        try {
                            mOutS.close();
                        } catch (IOException e1) {
                            LogUtil.e(debug, TAG, e.getMessage());
                            LogUtil.e(debug, TAG, e1.getMessage());
                            onDeviceConnectlistener.OnDeviceConnectedFailed();
                        }
                    }
                    if (mSocket != null) {
                        try {
                            mSocket.close();
                        } catch (IOException e1) {
                            LogUtil.e(debug, TAG, e.getMessage());
                            onDeviceConnectlistener.OnDeviceConnectedFailed();
                        }
                    }
                }
                LogUtil.i(debug, TAG, "【BluetoothUtil.connect success!!!】");
                onDeviceConnectlistener.OnDeviceConnectedSuccess();
            }
        }.start();

    }


    /**
     * 关闭流
     */
    public void close() {
        if (mOutS != null) {
            try {
                mOutS.close();
            } catch (IOException e) {
                LogUtil.e(debug, TAG, e.getMessage());
            }
        }

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                LogUtil.e(debug, TAG, e.getMessage());
            }
        }
    }

    /**
     * 发送数据
     *
     * @param data
     */
    public void sendData(final byte[] data) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    if (mOutS != null) {
                        // 写入数据
                        mOutS.write(data);
                        // 将缓冲区的数据发送出去，此方法必须调用
                        mOutS.flush();
                        String hexString = String.format("%02X", data[0]);
                        if (onLoglistener != null) {
                            onLoglistener.OnAddLog(hexString);
                        }
                    }
                } catch (IOException e) {
                    // UI控件必须在main线程执行，runOnUiThread内部实现即Handler消息处理机制
                }
            }
        }.start();

    }

    /**
     * @param address
     */
    boolean isConected = false;

    public void autoConnect(String address) {
        isConected = false;
        while (!isConected) {
            BluetoothDevice btDev = mBluetoothAdapter.getRemoteDevice(address);
            try {
                if (btDev.getBondState() == BluetoothDevice.BOND_NONE) {
                    Method creMethod = BluetoothDevice.class.getMethod("createBond");
                    creMethod.invoke(btDev);
                } else if (btDev.getBondState() == BluetoothDevice.BOND_BONDED) {
                    connect(btDev, new onDeviceConnectlistener() {
                        @Override
                        public void OnDeviceConnectedSuccess() {
                            isConected = true;
                        }

                        @Override
                        public void OnDeviceConnectedFailed() {
                            isConected = false;
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    public OnLoglistener onLoglistener;

    public void setOnLoglistener(OnLoglistener onLoglistener) {
        this.onLoglistener = onLoglistener;
    }


    /**
     * 注册广播接收器
     */
    public void addDeviceFondlistener(Activity activity, final onDeviceFondlistener onAddDeviceInterface) {
        mActivity = activity;
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND == action) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        if (device != null) {
                            if (onAddDeviceInterface != null) {
                                onAddDeviceInterface.OnAddDevice(device);
                            }
                        }
                    }
                }
            }
        };
        mActivity.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    private BroadcastReceiver mReceiver;

    /**
     * 注销广播接收器
     */
    public void unregReceiver() {
        if (mReceiver != null) {
            mActivity.unregisterReceiver(mReceiver);
        }
    }

    boolean readdata = true;

    public void stopReadDataFromServer() {
        readdata = false;
    }

    public void readDataFromServer(final OnDataRecivedlistener recivedlistener) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                readdata = true;
                while (readdata) {
                    if (mSocket != null) {
                        byte[] buffer = new byte[64];
                        try {
                            InputStream is = mSocket.getInputStream();
                            int cnt = is.read(buffer);
                            String s = new String(buffer, 0, cnt);
                            if (recivedlistener != null) {
                                recivedlistener.onDataRecived(s);
                            }
//                        is.close();
                            LogUtil.i(debug, TAG, "【BluetoothUtil.readDataFromServer()】【" + "收到服务端发来数据:" + s + "】");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();

    }
}
