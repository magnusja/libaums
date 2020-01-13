package me.jahnen.libaums.libusbcommunication

import android.hardware.usb.*
import android.util.Log
import com.github.mjdev.libaums.ErrNo
import com.github.mjdev.libaums.usb.UsbCommunication
import com.github.mjdev.libaums.usb.UsbCommunication.Companion.TRANSFER_TIMEOUT
import com.github.mjdev.libaums.usb.UsbCommunicationCreator
import java.io.IOException
import java.nio.ByteBuffer


class LibusbCommunication(
        private val usbManager: UsbManager,
        private val usbDevice: UsbDevice,
        override val usbInterface: UsbInterface,
        override val outEndpoint: UsbEndpoint,
        override val inEndpoint: UsbEndpoint
) : UsbCommunication {

    // used to save heap address of libusb device handle
    private var libUsbHandleArray = longArrayOf(0)
    private val libUsbHandle: Long
        get() = libUsbHandleArray[0]
    private var deviceConnection: UsbDeviceConnection?

    init {
        System.loadLibrary("libusbcom")

        deviceConnection = usbManager.openDevice(usbDevice)
                ?: throw IOException("deviceConnection is null!")

        if(!nativeInit(deviceConnection!!.fileDescriptor, libUsbHandleArray)) {
            throw IOException("libusb init failed")
        }

        val claim = deviceConnection!!.claimInterface(usbInterface, true)
        if (!claim) {
            throw IOException("could not claim interface!")
        }
        val ret = nativeClaimInterface(libUsbHandle, usbInterface.id)
        if (ret < 0) {
            throw IOException("libusb returned $ret in claim interface")
        }
    }

    private external fun nativeInit(fd: Int, handle: LongArray): Boolean
    private external fun nativeClaimInterface(handle: Long, interfaceNumber: Int): Int
    private external fun nativeClose(handle: Long, interfaceNumber: Int)
    private external fun nativeReset(handle: Long): Int
    private external fun nativeClearHalt(handle: Long, interfaceNumber: Int): Int
    private external fun nativeBulkTransfer(handle: Long, endpointAddress: Int, data: ByteArray, offset: Int, length: Int, timeout: Int): Int
    private external fun nativeControlTransfer(handle: Long, requestType: Int, request: Int, value: Int, index: Int, buffer: ByteArray, length: Int, timeout: Int): Int

    override fun bulkOutTransfer(src: ByteBuffer): Int {
        val transferred = nativeBulkTransfer(libUsbHandle, outEndpoint.address, src.array(), src.position(), src.remaining(), TRANSFER_TIMEOUT)
        if (transferred < 0) {
            throw IOException("libusb returned $transferred in bulk out transfer")
        }
        src.position(src.position() + transferred)
        return transferred
    }

    override fun bulkInTransfer(dest: ByteBuffer): Int {
        val transferred = nativeBulkTransfer(libUsbHandle, inEndpoint.address, dest.array(), dest.position(), dest.remaining(), TRANSFER_TIMEOUT)
        if (transferred < 0) {
            throw IOException("libusb returned $transferred in bulk in transfer")
        }
        dest.position(dest.position() + transferred)
        return transferred
    }

    override fun controlTransfer(requestType: Int, request: Int, value: Int, index: Int, buffer: ByteArray, length: Int): Int {
        val ret = nativeControlTransfer(libUsbHandle, requestType, request, value, index, buffer, length, TRANSFER_TIMEOUT)
        if (ret < 0) {
            throw IOException("libusb returned $ret in control transfer")
        }
        return ret
    }

    override fun resetDevice() {
        if (!deviceConnection!!.releaseInterface(usbInterface)) {
            Log.w(TAG, "Failed to release interface, errno: ${ErrNo.errno} ${ErrNo.errstr}")
        }

        val ret = nativeReset(libUsbHandle)
        // if LIBUSB_ERROR_NOT_FOUND might need reenumeration
        Log.d(TAG, "libusb reset returned $ret")

        if (!deviceConnection!!.claimInterface(usbInterface, true)) {
            throw IOException("Could not claim interface, errno: ${ErrNo.errno} ${ErrNo.errstr}")
        }
    }

    override fun clearFeatureHalt(endpoint: UsbEndpoint) {
        val ret = nativeClearHalt(libUsbHandle, usbInterface.id)
        Log.d(TAG, "libusb clearFeatureHalt returned $ret")
    }

    override fun close() {
        deviceConnection!!.releaseInterface(usbInterface)
        nativeClose(libUsbHandle, usbInterface.id)
        deviceConnection!!.close()
    }

    companion object {
        private val TAG = LibusbCommunication::class.java.simpleName
    }
}

class LibusbCommunicationCreator : UsbCommunicationCreator {
    override fun create(usbManager: UsbManager, usbDevice: UsbDevice, usbInterface: UsbInterface, outEndpoint: UsbEndpoint, inEndpoint: UsbEndpoint): UsbCommunication? {
        return LibusbCommunication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint)
    }

}