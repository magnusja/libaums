package com.github.mjdev.libaums.usb;

/**
 * Created by magnusja on 21/12/16.
 */

import android.annotation.TargetApi;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.Build;

/**
 * Usb communication which uses the newer API in Android Jelly Bean MR2 (API
 * level 18). It just delegates the calls to the {@link UsbDeviceConnection}
 * .
 *
 * @author mjahnen
 *
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class JellyBeanMr2Communication implements UsbCommunication {

    private UsbDeviceConnection deviceConnection;
    private UsbEndpoint outEndpoint;
    private UsbEndpoint inEndpoint;

    JellyBeanMr2Communication(UsbDeviceConnection deviceConnection, UsbEndpoint outEndpoint, UsbEndpoint inEndpoint) {
        this.deviceConnection = deviceConnection;
        this.outEndpoint = outEndpoint;
        this.inEndpoint = inEndpoint;
    }

    @Override
    public int bulkOutTransfer(byte[] buffer, int length) {
        return deviceConnection.bulkTransfer(outEndpoint, buffer, length, TRANSFER_TIMEOUT);
    }

    @Override
    public int bulkOutTransfer(byte[] buffer, int offset, int length) {
        return deviceConnection.bulkTransfer(outEndpoint, buffer, offset, length,
                TRANSFER_TIMEOUT);
    }

    @Override
    public int bulkInTransfer(byte[] buffer, int length) {
        return deviceConnection.bulkTransfer(inEndpoint, buffer, length, TRANSFER_TIMEOUT);
    }

    @Override
    public int bulkInTransfer(byte[] buffer, int offset, int length) {
        return deviceConnection.bulkTransfer(inEndpoint, buffer, offset, length,
                TRANSFER_TIMEOUT);
    }
}
