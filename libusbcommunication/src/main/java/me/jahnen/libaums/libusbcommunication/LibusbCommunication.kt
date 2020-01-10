package me.jahnen.libaums.libusbcommunication

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.github.mjdev.libaums.usb.UsbCommunication
import com.github.mjdev.libaums.usb.UsbCommunicationCreator
import java.io.IOException
import java.nio.ByteBuffer

class LibusbCommunication(
        private val usbManager: UsbManager,
        private val usbDevice: UsbDevice,
        private val usbInterface: UsbInterface,
        override val outEndpoint: UsbEndpoint,
        override val inEndpoint: UsbEndpoint
) : UsbCommunication {

    init {
        System.loadLibrary("libusbcom")
        if(!nativeInit()) {
            throw IOException("libusb init failed")
        }
        if(!nativeOpen()) {
            throw IOException("libusb open failed")
        }
    }

    private external fun nativeInit(): Boolean
    private external fun nativeOpen(): Boolean

    override fun bulkOutTransfer(src: ByteBuffer): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bulkInTransfer(dest: ByteBuffer): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun controlTransfer(requestType: Int, request: Int, value: Int, index: Int, buffer: ByteArray, length: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resetRecovery() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bulkOnlyMassStorageReset() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearFeatureHalt(endpoint: UsbEndpoint) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class LibusbCommunicationCreator : UsbCommunicationCreator {
    override fun create(usbManager: UsbManager, usbDevice: UsbDevice, usbInterface: UsbInterface, outEndpoint: UsbEndpoint, inEndpoint: UsbEndpoint): UsbCommunication? {
        return LibusbCommunication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint)
    }

}