package com.github.mjdev.libaums.usb;

/**
 * Created by magnusja on 21/12/16.
 */

import android.annotation.TargetApi;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.os.Build;

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
    private UsbInterface usbInterface;

    JellyBeanMr2Communication(UsbDeviceConnection deviceConnection, UsbEndpoint outEndpoint, UsbEndpoint inEndpoint, UsbInterface usbInterface) {
        this.deviceConnection = deviceConnection;
        this.outEndpoint = outEndpoint;
        this.inEndpoint = inEndpoint;
        this.usbInterface = usbInterface;
    }

    @Override
    public int bulkOutTransfer(ByteBuffer src) throws IOException {
        int result = deviceConnection.bulkTransfer(outEndpoint,
                src.array(), src.position(), src.remaining(), TRANSFER_TIMEOUT);

        if (result == -1) {
            deviceConnection.controlTransfer(0b00100001, 0b11111111, 0, usbInterface.getId(), null, 0, 5000);
            result = deviceConnection.bulkTransfer(outEndpoint,
                    src.array(), src.position(), src.remaining(), TRANSFER_TIMEOUT);
            if (result == -1) {
                throw new IOException("Could not write to device, result == -1");
            }
        }

        src.position(src.position() + result);
        return result;
    }

    @Override
    public int bulkInTransfer(ByteBuffer dest) throws IOException {
        int result = deviceConnection.bulkTransfer(inEndpoint,
                dest.array(), dest.position(), dest.remaining(), TRANSFER_TIMEOUT);

        if (result == -1) {
            deviceConnection.controlTransfer(0b00100001, 0b11111111, 0, usbInterface.getId(), null, 0, 5000);
            result = deviceConnection.bulkTransfer(inEndpoint,
                    dest.array(), dest.position(), dest.remaining(), TRANSFER_TIMEOUT);
            if (result == -1) {
                throw new IOException("Could not read from device, result == -1");
            }
        }

        dest.position(dest.position() + result);
        return result;
    }
}
