package com.JuserZhang.BluetoothCar.util;

import android.content.Context;
import android.widget.Toast;

/**
 * 消息弹框工具类
 */
public class ToastUtil {
    private static Context mContext;
    public static void init(Context context) {
        mContext = context;
    }
    public static void longToast( String text) {
        Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
    }
    public static void longToast( int resId) {
        Toast.makeText(mContext, resId, Toast.LENGTH_LONG).show();
    }

    public static void shortToast( String text) {
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
    }
    public static void shortToast( int resId) {
        Toast.makeText(mContext, resId, Toast.LENGTH_SHORT).show();
    }
}
