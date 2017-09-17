package com.JuserZhang.BluetoothCar;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.ListView;

import com.JuserZhang.BluetoothCar.adapter.DeviceListAdapter;
import com.JuserZhang.BluetoothCar.util.ToastUtil;
import com.JuserZhang.BluetoothCar.util.WaitDialog;
import com.JuserZhang.BluetoothCar.util.bluetooth.BluetoothUtil;
import com.JuserZhang.BluetoothCar.util.bluetooth.onDeviceConnectlistener;
import com.JuserZhang.BluetoothCar.util.bluetooth.onDeviceFondlistener;
import com.JuserZhang.BluetoothCar.widget.WhorlView;

public class DeviceScanActivity extends ListActivity {
    // 调试用
    private static final String TAG = "DeviceScanActivity";

    // 开启蓝牙请求码
    private static final int REQUEST_ENABLE = 0;

    // 停止扫描蓝牙消息头
    private static final int WHAT_CANCEL_DISCOVERY = 1;
    // 更新列表消息头
    private static final int WHAT_DEVICE_UPDATE = 2;

    // 扫描间隔时间
    private static final int SCAN_PERIOD = 30 * 1000;

    private DeviceListAdapter mLeDeviceListAdapter = null;
    // 蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter = null;
    // 螺纹进度条
    private WhorlView mWhorlView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        init();
        ToastUtil.init(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLeDeviceListAdapter = new DeviceListAdapter(this);
        // 设置列表适配器，注：调用此方法必须继承ListActivity
        setListAdapter(mLeDeviceListAdapter);
        BluetoothUtil.getInstance().startScanDevice();
        mWhorlView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        BluetoothUtil.getInstance().stopScanDevice();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        BluetoothUtil.getInstance().unregReceiver();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 线程自杀
        if (requestCode == REQUEST_ENABLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            Process.killProcess(Process.myPid());
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) {
            return;
        }
        final ProgressDialog progressDialog = WaitDialog.show(this, "提示", "连接中...");
        BluetoothUtil.getInstance().connect(device, new onDeviceConnectlistener() {
            @Override
            public void OnDeviceConnectedSuccess() {
                ToastUtil.longToast("conneced");
                progressDialog.cancel();
                // 执行Intent跳转并携带数据
                Intent intent = new Intent(DeviceScanActivity.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable("device", device);
                intent.putExtras(bundle);
                startActivity(intent);
            }
            @Override
            public void OnDeviceConnectedFailed() {
                progressDialog.cancel();
                ToastUtil.shortToast("连接失败");
            }
        });

    }


    /**
     * 初始化
     */
    private void init() {
        mWhorlView = (WhorlView) findViewById(R.id.whorl_view);
        // 开启动画
        mWhorlView.start();
        // 初始化本地蓝牙设备
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 检测蓝牙设备是否开启，如果未开启，发起Intent并回调
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE);
        }
        BluetoothUtil.getInstance().startScanDevice();
        BluetoothUtil.getInstance().addDeviceFondlistener(this,new onDeviceFondlistener() {
            @Override
            public void OnAddDevice(BluetoothDevice device) {
                mLeDeviceListAdapter.addDevice(device);
                // 刷新列表
                mLeDeviceListAdapter.notifyDataSetChanged();
                mWhorlView.setVisibility(View.GONE);
            }
        });
    }


}
