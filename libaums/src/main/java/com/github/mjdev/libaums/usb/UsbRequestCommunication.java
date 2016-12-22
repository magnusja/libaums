package com.github.mjdev.libaums.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by magnusja on 21/12/16.
 */

public class UsbRequestCommunication implements UsbCommunication {

    UsbDeviceConnection deviceConnection;
    UsbRequest outRequest;
    UsbRequest inRequest;

    public UsbRequestCommunication(UsbDeviceConnection deviceConnection, UsbEndpoint outEndpoint, UsbEndpoint inEndpoint) {
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
        outRequest.queue(src, length);

        if (deviceConnection.requestWait() == outRequest) {
            return length;
        }

        throw new IOException("requestWait failed!");
    }

    @Override
    public synchronized int bulkInTransfer(ByteBuffer dest) throws IOException {
        int length = dest.remaining();
        inRequest.queue(dest, length);

        if (deviceConnection.requestWait() == inRequest) {
            return length;
        }

        throw new IOException("requestWait failed!");
    }
}
