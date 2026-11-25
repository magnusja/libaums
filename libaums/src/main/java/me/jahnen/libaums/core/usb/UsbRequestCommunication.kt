package me.jahnen.libaums.core.usb

import android.hardware.usb.*
import android.os.Build
import androidx.annotation.RequiresApi

import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeoutException

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

    /** The no-arg `UsbDeviceConnection.requestWait()` this class used to call at every site below
     *  blocks forever if the hardware never delivers a completion event for the queued request —
     *  confirmed on real hardware (2026-11-23/24): the very first SCSI command over this
     *  transport hung indefinitely, with no exception and no way for any caller's own timeout to
     *  help, since the hang is inside this blocking call itself. The timed overload (available
     *  since the same API level — O — this class already requires) throws [TimeoutException]
     *  instead of blocking forever, so a stalled transfer becomes a normal, catchable I/O failure
     *  the rest of the app's retry logic can react to, same as every other transport here. */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun waitFor(request: UsbRequest): UsbRequest {
        val completed = try {
            deviceConnection!!.requestWait(UsbCommunication.TRANSFER_TIMEOUT.toLong())
        } catch (e: TimeoutException) {
            throw IOException("requestWait timed out after ${UsbCommunication.TRANSFER_TIMEOUT}ms", e)
        }
        if (completed !== request) {
            throw IOException("requestWait failed! Request: $completed")
        }
        return completed
    }


    @Throws(IOException::class)
    @RequiresApi(Build.VERSION_CODES.O)
    fun bulkOutTransferApiO(src: ByteBuffer): Int {
        val oldPosition = src.position();
        if (!outRequest.queue(src)) {
            throw IOException("Error queueing request.")
        }

        waitFor(outRequest)
        return src.position() - oldPosition
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

        waitFor(outRequest)
        src.position(oldPosition + workaroundBuffer.position())
        return workaroundBuffer.position()
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

        waitFor(inRequest)
        workaroundBuffer.flip()
        dest.put(workaroundBuffer)
        return workaroundBuffer.limit()
    }

    @Throws(IOException::class)
    @RequiresApi(Build.VERSION_CODES.O)
    fun bulkInTransferApiO(dest: ByteBuffer): Int {
        val oldPosition = dest.position();
        if (!inRequest.queue(dest)) {
            throw IOException("Error queueing request.")
        }

        waitFor(inRequest)
        return dest.position() - oldPosition
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
