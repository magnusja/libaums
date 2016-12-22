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

        // workaround: UsbRequest.queue always reads at position 0 :/
        ByteBuffer tmp = ByteBuffer.wrap(src.array(), src.position(), length);

        if (!outRequest.queue(tmp, length)) {
            throw new IOException("Error queueing request.");
        }

        UsbRequest request = deviceConnection.requestWait();
        if (request == outRequest) {
            src.position(src.position() + tmp.position());
            return tmp.position();
        }

        throw new IOException("requestWait failed! Request: " + request);
    }

    @Override
    public synchronized int bulkInTransfer(ByteBuffer dest) throws IOException {
        int length = dest.remaining();

        // workaround: UsbRequest.queue always writes at position 0 :/
        ByteBuffer tmp = ByteBuffer.wrap(dest.array(), dest.position(), length);
        if(!inRequest.queue(tmp, length)) {
            throw new IOException("Error queueing request.");
        }

        UsbRequest request = deviceConnection.requestWait();
        if (request == inRequest) {
            dest.position(dest.position() + tmp.position());
            return tmp.position();
        }

        throw new IOException("requestWait failed! Request: " + request);
    }
}
