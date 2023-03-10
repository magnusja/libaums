package me.jahnen.libaums.core.usb

import android.hardware.usb.*
import android.os.Build
import androidx.annotation.RequiresApi

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


    @Throws(IOException::class)
    @RequiresApi(Build.VERSION_CODES.O)
    fun bulkOutTransferApiO(src: ByteBuffer): Int {
        val oldPosition = src.position();
        if (!outRequest.queue(src)) {
            throw IOException("Error queueing request.")
        }

        val request = deviceConnection!!.requestWait()
        if (request === outRequest) {
            return workaroundBuffer.position() - oldPosition
        }

        throw IOException("requestWait failed! Request: $request")
    }


    @Throws(IOException::class)
    fun bulkOutTransferApiLesserO(src: ByteBuffer): Int {
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
    override fun bulkOutTransfer(src: ByteBuffer): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bulkOutTransferApiO(src)
        } else {
            bulkOutTransferApiLesserO(src)
        }
    }


    @Throws(IOException::class)
    fun bulkInTransferApiLesserO(dest: ByteBuffer): Int {
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

    @Throws(IOException::class)
    @RequiresApi(Build.VERSION_CODES.O)
    fun bulkInTransferApiO(dest: ByteBuffer): Int {
        val oldPosition = dest.position();
        if (!inRequest.queue(dest)) {
            throw IOException("Error queueing request.")
        }

        val request = deviceConnection!!.requestWait()
        if (request === outRequest) {
            return workaroundBuffer.position() - oldPosition
        }

        throw IOException("requestWait failed! Request: $request")
    }

    @Synchronized
    @Throws(IOException::class)
    override fun bulkInTransfer(dest: ByteBuffer): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bulkInTransferApiO(dest)
        } else {
            bulkInTransferApiLesserO(dest)
        }
    }
}
