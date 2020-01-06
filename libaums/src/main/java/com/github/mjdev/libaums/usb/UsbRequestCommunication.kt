package com.github.mjdev.libaums.usb

import android.hardware.usb.*

import java.io.IOException
import java.nio.ByteBuffer

/**
 * Created by magnusja on 21/12/16.
 */

internal class UsbRequestCommunication(
        usbManager: UsbManager,
        usbDevice: UsbDevice,
        usbInterface: UsbInterface,
        outEndpoint: UsbEndpoint,
        inEndpoint: UsbEndpoint
) : AndroidUsbCommunication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint) {

    private val outRequest = UsbRequest().apply { initialize(deviceConnection, outEndpoint) }
    private val inRequest = UsbRequest().apply { initialize(deviceConnection, inEndpoint) }
    private val workaroundBuffer = ByteBuffer.allocate(1024 * 32 * 4)

    
    @Synchronized
    @Throws(IOException::class)
    override fun bulkOutTransfer(src: ByteBuffer): Int {
        val length = src.remaining()
        val oldPosition = src.position()

        // workaround: UsbRequest.queue always reads at position 0 :/
        workaroundBuffer.clear()
        workaroundBuffer.put(src)

        if (!outRequest.queue(workaroundBuffer, length)) {
            throw IOException("Error queueing request.")
        }

        val request = deviceConnection!!.requestWait()
        if (request === outRequest) {
            src.position(oldPosition + workaroundBuffer.position())
            return workaroundBuffer.position()
        }

        throw IOException("requestWait failed! Request: $request")
    }

    @Synchronized
    @Throws(IOException::class)
    override fun bulkInTransfer(dest: ByteBuffer): Int {
        val length = dest.remaining()

        // workaround: UsbRequest.queue always writes at position 0 :/
        workaroundBuffer.clear()
        workaroundBuffer.limit(length)

        if (!inRequest.queue(workaroundBuffer, length)) {
            throw IOException("Error queueing request.")
        }

        val request = deviceConnection!!.requestWait()
        if (request === inRequest) {
            workaroundBuffer.flip()
            dest.put(workaroundBuffer)
            return workaroundBuffer.limit()
        }

        throw IOException("requestWait failed! Request: $request")
    }
}
