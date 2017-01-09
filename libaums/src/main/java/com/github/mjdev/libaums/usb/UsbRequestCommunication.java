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
    private ByteBuffer workaroundBuffer = ByteBuffer.allocate(1024 * 32 * 4);

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

        // workaround: UsbRequest.queue always reads at position 0 :/
        workaroundBuffer.clear();
        workaroundBuffer.put(src);

        if (!outRequest.queue(workaroundBuffer, length)) {
            throw new IOException("Error queueing request.");
        }

        UsbRequest request = deviceConnection.requestWait();
        if (request == outRequest) {
            src.position(oldPosition + workaroundBuffer.position());
            return workaroundBuffer.position();
        }

        throw new IOException("requestWait failed! Request: " + request);
    }

    @Override
    public synchronized int bulkInTransfer(ByteBuffer dest) throws IOException {
        int length = dest.remaining();

        // workaround: UsbRequest.queue always writes at position 0 :/
        workaroundBuffer.clear();
        workaroundBuffer.limit(length);

        if(!inRequest.queue(workaroundBuffer, length)) {
            throw new IOException("Error queueing request.");
        }

        UsbRequest request = deviceConnection.requestWait();
        if (request == inRequest) {
            workaroundBuffer.flip();
            dest.put(workaroundBuffer);
            return workaroundBuffer.limit();
        }

        throw new IOException("requestWait failed! Request: " + request);
    }
}
