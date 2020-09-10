package com.github.mjdev.libaums.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import java.io.IOException
import java.nio.ByteBuffer

/**
 * On Android API level lower 18 (Jelly Bean MR2) we cannot specify a start
 * offset in the source/destination array. Because of that we have to use
 * this workaround, where we have to copy the data every time offset is non
 * zero.
 *
 * @author mjahnen
 */
internal class HoneyCombMr1Communication(
        usbManager: UsbManager,
        usbDevice: UsbDevice,
        usbInterface: UsbInterface,
        outEndpoint: UsbEndpoint,
        inEndpoint: UsbEndpoint
) : AndroidUsbCommunication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint) {

    @Throws(IOException::class)
    override fun bulkOutTransfer(src: ByteBuffer): Int {
        val offset = src.position()

        if (offset == 0) {
            val result = deviceConnection!!.bulkTransfer(outEndpoint,
                    src.array(), src.remaining(), UsbCommunication.TRANSFER_TIMEOUT)

            if (result == -1) {
                throw IOException("Could not write to device, result == -1")
            }

            src.position(src.position() + result)
            return result
        }

        val tmpBuffer = ByteArray(src.remaining())
        System.arraycopy(src.array(), offset, tmpBuffer, 0, src.remaining())
        val result = deviceConnection!!.bulkTransfer(outEndpoint,
                tmpBuffer, src.remaining(), UsbCommunication.TRANSFER_TIMEOUT)

        if (result == -1) {
            throw IOException("Could not write to device, result == -1")
        }

        src.position(src.position() + result)
        return result
    }

    @Throws(IOException::class)
    override fun bulkInTransfer(dest: ByteBuffer): Int {
        val offset = dest.position()

        if (offset == 0) {
            val result = deviceConnection!!.bulkTransfer(inEndpoint,
                    dest.array(), dest.remaining(), UsbCommunication.TRANSFER_TIMEOUT)

            if (result == -1) {
                throw IOException("Could read from to device, result == -1")
            }

            dest.position(dest.position() + result)
            return result

        }

        val tmpBuffer = ByteArray(dest.remaining())
        val result = deviceConnection!!.bulkTransfer(inEndpoint, tmpBuffer, dest.remaining(), UsbCommunication.TRANSFER_TIMEOUT)

        if (result == -1) {
            throw IOException("Could not read from device, result == -1")
        }

        System.arraycopy(tmpBuffer, 0, dest.array(), offset, result)
        dest.position(dest.position() + result)
        return result
    }
}
