package com.github.mjdev.libaums.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

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
    public int bulkOutTransfer(byte[] buffer, int length) {
        return deviceConnection.bulkTransfer(outEndpoint, buffer, length, TRANSFER_TIMEOUT);
    }

    @Override
    public int bulkOutTransfer(byte[] buffer, int offset, int length) {
        if (offset == 0)
            return deviceConnection.bulkTransfer(outEndpoint, buffer, length, TRANSFER_TIMEOUT);

        byte[] tmpBuffer = new byte[length];
        System.arraycopy(buffer, offset, tmpBuffer, 0, length);
        return deviceConnection.bulkTransfer(outEndpoint, tmpBuffer, length,
                TRANSFER_TIMEOUT);
    }

    @Override
    public int bulkInTransfer(byte[] buffer, int length) {
        return deviceConnection.bulkTransfer(inEndpoint, buffer, length, TRANSFER_TIMEOUT);
    }

    @Override
    public int bulkInTransfer(byte[] buffer, int offset, int length) {
        if (offset == 0)
            return deviceConnection.bulkTransfer(inEndpoint, buffer, length, TRANSFER_TIMEOUT);

        byte[] tmpBuffer = new byte[length];
        int result = deviceConnection.bulkTransfer(inEndpoint, tmpBuffer, length,
                TRANSFER_TIMEOUT);
        System.arraycopy(tmpBuffer, 0, buffer, offset, length);
        return result;
    }
}
