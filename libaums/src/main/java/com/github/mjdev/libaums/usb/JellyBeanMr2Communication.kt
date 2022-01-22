package com.github.mjdev.libaums.usb

/**
 * Created by magnusja on 21/12/16.
 */

import android.annotation.TargetApi
import android.hardware.usb.*
import android.os.Build
import android.system.OsConstants.EPIPE
import com.github.mjdev.libaums.ErrNo
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Usb communication which uses the newer API in Android Jelly Bean MR2 (API
 * level 18). It just delegates the calls to the [UsbDeviceConnection]
 * .
 *
 * @author mjahnen
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
internal class JellyBeanMr2Communication(
        usbManager: UsbManager,
        usbDevice: UsbDevice,
        usbInterface: UsbInterface,
        outEndpoint: UsbEndpoint,
        inEndpoint: UsbEndpoint
) : AndroidUsbCommunication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint) {

    @Throws(IOException::class)
    override fun bulkOutTransfer(src: ByteBuffer): Int {
        val result = deviceConnection!!.bulkTransfer(outEndpoint,
                src.array(), src.position(), src.remaining(), UsbCommunication.TRANSFER_TIMEOUT)

        if (result == -1) {
            when (ErrNo.errno) {
                EPIPE -> throw PipeException()
                else -> throw IOException("Could not read from device, result == -1 errno " + ErrNo.errno + " " + ErrNo.errstr)
            }
        }

        src.position(src.position() + result)
        return result
    }

    @Throws(IOException::class)
    override fun bulkInTransfer(dest: ByteBuffer): Int {
        val result = deviceConnection!!.bulkTransfer(inEndpoint,
                dest.array(), dest.position(), dest.remaining(), UsbCommunication.TRANSFER_TIMEOUT)

        if (result == -1) {
            when (ErrNo.errno) {
                EPIPE -> throw PipeException()
                else -> throw IOException("Could not read from device, result == -1 errno " + ErrNo.errno + " " + ErrNo.errstr)
            }
        }

        dest.position(dest.position() + result)
        return result
    }
}
