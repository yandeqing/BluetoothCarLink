package com.JuserZhang.BluetoothCar.util;

import java.lang.reflect.Method;

/**
 * 日志打印工具类
 */
public class LogUtil {
    // 全局Log开关
    private static final boolean DEBUG = false;

    public static void v(boolean debug, String tag, String msg) {
        if (DEBUG) {
            if (debug) {
                if (debug) print(V, tag, msg);
            }
        }
    }

    public static void d(boolean debug, String tag, String msg) {
        if (DEBUG) {
            if (debug) print(D, tag, msg);
        }
    }

    public static void i(boolean debug, String tag, String msg) {
        if (DEBUG) {
            if (debug) print(I, tag, msg);
        }
    }

    public static void w(boolean debug, String tag, String msg) {
        if (DEBUG) {
            if (debug) print(W, tag, msg);
        }
    }

    public static void e(boolean debug, String tag, String msg) {
        if (DEBUG) {
            if (debug) print(E, tag, msg);
        }
    }


    /**
     * Print log for define method. When information is too long, the Logger can also complete printing. The equivalent of "{@code android.util.Log.i("Tag", "Message")}" "{@code com.yolanda.nohttp.Logger.if(debug)print("i", "Tag", "Message")}".
     *
     * @param method  such as "{@code v, i, d, w, e, wtf}".
     * @param tag     tag.
     * @param message message.
     */
    public static void print(String method, String tag, String message) {
        if (message == null) {
            message = "null";
        }
        message = decodeUnicode(message);
        int strLength = message.length();
        if (strLength == 0)
            invokePrint(method, tag, message);
        else {
            for (int i = 0; i < strLength / maxLength + (strLength % maxLength > 0 ? 1 : 0); i++) {
                int end = (i + 1) * maxLength;
                if (strLength >= end) {
                    invokePrint(method, tag, message.substring(end - maxLength, end));
                } else {
                    invokePrint(method, tag, message.substring(end - maxLength));
                }
            }
        }
    }


    /**
     * Default length.
     */
    private static final int MAX_LENGTH = 1024;
    /**
     * Length of message.
     */
    private static int maxLength = MAX_LENGTH;


    /**
     * Through the reflection to call the print method.
     *
     * @param method  such as "{@code v, i, d, w, e, wtf}".
     * @param tag     tag.
     * @param message message.
     */
    public static void invokePrint(String method, String tag, String message) {
        try {
            Class<android.util.Log> logClass = android.util.Log.class;
            Method logMethod = logClass.getMethod(method, String.class, String.class);
            logMethod.setAccessible(true);
            logMethod.invoke(null, tag, message);
        } catch (Exception e) {
            System.out.println(tag + ": " + message);
        }
    }

    public static final String V = "v";
    public static final String I = "i";
    public static final String D = "d";
    public static final String W = "w";
    public static final String E = "e";


    /**
     * 解码unicode
     *
     * @param theString
     * @return
     */
    public static String decodeUnicode(String theString) {
        StringBuffer outBuffer = new StringBuffer();
        try {
            char aChar;
            int len = theString.length();
            outBuffer = new StringBuffer(len);
            for (int x = 0; x < len; ) {
                aChar = theString.charAt(x++);
                if (aChar == '\\') {
                    aChar = theString.charAt(x++);
                    if (aChar == 'u') {
                        int value = 0;
                        for (int i = 0; i < 4; i++) {
                            aChar = theString.charAt(x++);
                            switch (aChar) {
                                case '0':
                                case '1':
                                case '2':
                                case '3':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                case '8':
                                case '9':
                                    value = (value << 4) + aChar - '0';
                                    break;
                                case 'a':
                                case 'b':
                                case 'c':
                                case 'd':
                                case 'e':
                                case 'f':
                                    value = (value << 4) + 10 + aChar - 'a';
                                    break;
                                case 'A':
                                case 'B':
                                case 'C':
                                case 'D':
                                case 'E':
                                case 'F':
                                    value = (value << 4) + 10 + aChar - 'A';
                                    break;
                                default:
                                    throw new IllegalArgumentException(
                                            "Malformed   \\uxxxx   encoding.");
                            }
                        }
                        outBuffer.append((char) value);
                    } else {
                        if (aChar == 't')
                            aChar = '\t';
                        else if (aChar == 'r')
                            aChar = '\r';
                        else if (aChar == 'n')
                            aChar = '\n';
                        else if (aChar == 'f')
                            aChar = '\f';
                        outBuffer.append(aChar);
                    }
                } else
                    outBuffer.append(aChar);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return outBuffer.toString();
    }
}
