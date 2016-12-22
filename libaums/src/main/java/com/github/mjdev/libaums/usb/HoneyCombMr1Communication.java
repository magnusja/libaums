package com.github.mjdev.libaums.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * On Android API level lower 18 (Jelly Bean MR2) we cannot specify a start
 * offset in the source/destination array. Because of that we have to use
 * this workaround, where we have to copy the data every time offset is non
 * zero.
 *
 * @author mjahnen
 *
 */
class HoneyCombMr1Communication implements UsbCommunication {

    private UsbDeviceConnection deviceConnection;
    private UsbEndpoint outEndpoint;
    private UsbEndpoint inEndpoint;

    HoneyCombMr1Communication(UsbDeviceConnection deviceConnection, UsbEndpoint outEndpoint, UsbEndpoint inEndpoint) {
        this.deviceConnection = deviceConnection;
        this.outEndpoint = outEndpoint;
        this.inEndpoint = inEndpoint;
    }

    @Override
    public int bulkOutTransfer(ByteBuffer src) throws IOException {
        int offset = src.position();

        if (offset == 0) {
            int result =  deviceConnection.bulkTransfer(outEndpoint,
                    src.array(), src.remaining(), TRANSFER_TIMEOUT);

            if (result == -1) {
                throw new IOException("Could not write to device, result == -1");
            }

            src.position(src.position() + result);
            return result;
        }

        byte[] tmpBuffer = new byte[src.remaining()];
        System.arraycopy(src.array(), offset, tmpBuffer, 0, src.remaining());
        int result =  deviceConnection.bulkTransfer(outEndpoint,
                tmpBuffer, src.remaining(), TRANSFER_TIMEOUT);

        if (result == -1) {
            throw new IOException("Could not write to device, result == -1");
        }

        src.position(src.position() + result);
        return result;
    }

    @Override
    public int bulkInTransfer(ByteBuffer dest) throws IOException {
        int offset = dest.position();

        if (offset == 0) {
            int result = deviceConnection.bulkTransfer(inEndpoint,
                    dest.array(), dest.remaining(), TRANSFER_TIMEOUT);

            if (result == -1) {
                throw new IOException("Could read from to device, result == -1");
            }

            dest.position(dest.position() + result);
            return result;

        }

        byte[] tmpBuffer = new byte[dest.remaining()];
        int result = deviceConnection.bulkTransfer(inEndpoint, tmpBuffer, dest.remaining(), TRANSFER_TIMEOUT);

        if (result == -1) {
            throw new IOException("Could not read from device, result == -1");
        }

        System.arraycopy(tmpBuffer, 0, dest.array(), offset, result);
        dest.position(dest.position() + result);
        return result;
    }
}
