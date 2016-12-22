package com.github.mjdev.libaums.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by magnusja on 21/12/16.
 */

class UsbRequestCommunication implements UsbCommunication {

    private UsbDeviceConnection deviceConnection;
    private UsbRequest outRequest;
    private UsbRequest inRequest;

    UsbRequestCommunication(UsbDeviceConnection deviceConnection, UsbEndpoint outEndpoint, UsbEndpoint inEndpoint) {
        this.deviceConnection = deviceConnection;
        UsbRequest request = new UsbRequest();
        request.initialize(deviceConnection, outEndpoint);
        this.outRequest = request;

        request = new UsbRequest();
        request.initialize(deviceConnection, inEndpoint);
        this.inRequest = request;
    }

    @Override
    public synchronized int bulkOutTransfer(ByteBuffer src) throws IOException {
        int length = src.remaining();
        int oldPosition = src.position();
        if (!outRequest.queue(src, length)) {
            throw new IOException("Error queueing request.");
        }

        if (deviceConnection.requestWait() == outRequest) {
            return src.position() - oldPosition;
        }

        throw new IOException("requestWait failed!");
    }

    @Override
    public synchronized int bulkInTransfer(ByteBuffer dest) throws IOException {
        int length = dest.remaining();
        int oldPosition = dest.position();
        if(!inRequest.queue(dest, length)) {
            throw new IOException("Error queueing request.");
        }

        if (deviceConnection.requestWait() == inRequest) {
            return dest.position() - oldPosition;
        }

        throw new IOException("requestWait failed!");
    }
}
