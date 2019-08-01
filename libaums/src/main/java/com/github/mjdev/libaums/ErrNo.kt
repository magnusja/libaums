package com.github.mjdev.libaums

import android.util.Log

/**
 * Created by magnusja on 5/19/17.
 */

object ErrNo {

    private val TAG = "ErrNo"
    private var isInited = true


    val errno: Int
        get() = if (isInited) {
            errnoNative
        } else {
            1337
        }

    val errstr: String
        get() = if (isInited) {
            errstrNative
        } else {
            "errno-lib could not be loaded!"
        }

    private val errnoNative: Int
        external get
    private val errstrNative: String
        external get

    init {
        try {
            System.loadLibrary("errno-lib")
        } catch (e: UnsatisfiedLinkError) {
            isInited = false
            Log.e(TAG, "could not load errno-lib", e)
        }

    }
}
