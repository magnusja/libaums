package com.github.mjdev.libaums;

/**
 * Created by magnusja on 5/19/17.
 */

public class ErrNo {
    static {
        System.loadLibrary("errno-lib");
    }

    public static native int getErrno();
    public static native String getErrstr();
}
