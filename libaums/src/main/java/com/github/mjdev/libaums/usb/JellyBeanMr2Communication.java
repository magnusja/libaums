package com.github.mjdev.libaums.usb;

/**
 * Created by magnusja on 21/12/16.
 */

import android.annotation.TargetApi;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.Build;
import android.system.ErrnoException;

import com.github.mjdev.libaums.ErrNo;

import java.io.IOException;
import java.nio.ByteBuffer;

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
    public int bulkOutTransfer(ByteBuffer src) throws IOException {
        int result = deviceConnection.bulkTransfer(outEndpoint,
                src.array(), src.position(), src.remaining(), TRANSFER_TIMEOUT);

        if (result == -1) {
            throw new IOException("Could not write to device, result == -1 errno " + ErrNo.getErrno() + " " + ErrNo.getErrstr());
        }

        src.position(src.position() + result);
        return result;
    }

    @Override
    public int bulkInTransfer(ByteBuffer dest) throws IOException {
        int result = deviceConnection.bulkTransfer(inEndpoint,
                dest.array(), dest.position(), dest.remaining(), TRANSFER_TIMEOUT);

        if (result == -1) {
            throw new IOException("Could not read from device, result == -1 errno " + ErrNo.getErrno() + " " + ErrNo.getErrstr());
        }

        dest.position(dest.position() + result);
        return result;
    }
}
