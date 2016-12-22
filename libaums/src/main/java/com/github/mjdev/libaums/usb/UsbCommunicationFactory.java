package com.github.mjdev.libaums.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.Build;
import android.util.Log;

/**
 * Created by magnusja on 21/12/16.
 */

public class UsbCommunicationFactory {

    private static final String TAG = UsbCommunicationFactory.class.getSimpleName();

    public static UsbCommunication createUsbCommunication(UsbDeviceConnection deviceConnection, UsbEndpoint outEndpoint, UsbEndpoint inEndpoint) {
        UsbCommunication communication;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            communication = new JellyBeanMr2Communication(deviceConnection, outEndpoint, inEndpoint);
        } else {
            Log.i(TAG, "using workaround usb communication");
            communication = new HoneyCombMr1Communication(deviceConnection, outEndpoint, inEndpoint);
        }

        return communication;
    }
}
