package com.github.mjdev.libaums;

import android.util.Log;

/**
 * Created by magnusja on 5/19/17.
 */

public class ErrNo {

    private static String TAG = ErrNo.class.getSimpleName();
    private static boolean isInited = true;

    static {
        try {
            System.loadLibrary("errno-lib");
        } catch(UnsatisfiedLinkError e) {
            isInited = false;
            Log.e(TAG, "could not load errno-lib", e);
        }
    }


    public static int getErrno() {
        if(isInited) {
            return getErrnoNative();
        } else {
            return 1337;
        }
    }

    public static String getErrstr() {
        if(isInited) {
            return getErrstrNative();
        } else {
            return "errno-lib could not be loaded!";
        }
    }

    public static native int getErrnoNative();
    public static native String getErrstrNative();
}
