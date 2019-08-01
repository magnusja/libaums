package com.github.mjdev.libaums.usb

/**
 * Created by magnusja on 21/12/16.
 */

import android.annotation.TargetApi
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.os.Build
import android.system.ErrnoException

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
internal class JellyBeanMr2Communication(private val deviceConnection: UsbDeviceConnection, private val outEndpoint: UsbEndpoint, private val inEndpoint: UsbEndpoint) : UsbCommunication {

    @Throws(IOException::class)
    override fun bulkOutTransfer(src: ByteBuffer): Int {
        val result = deviceConnection.bulkTransfer(outEndpoint,
                src.array(), src.position(), src.remaining(), UsbCommunication.TRANSFER_TIMEOUT)

        if (result == -1) {
            throw IOException("Could not write to device, result == -1 errno " + ErrNo.errno + " " + ErrNo.errstr)
        }

        src.position(src.position() + result)
        return result
    }

    @Throws(IOException::class)
    override fun bulkInTransfer(dest: ByteBuffer): Int {
        val result = deviceConnection.bulkTransfer(inEndpoint,
                dest.array(), dest.position(), dest.remaining(), UsbCommunication.TRANSFER_TIMEOUT)

        if (result == -1) {
            throw IOException("Could not read from device, result == -1 errno " + ErrNo.errno + " " + ErrNo.errstr)
        }

        dest.position(dest.position() + result)
        return result
    }
}
