package com.github.mjdev.libaums.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.util.Log
import com.github.mjdev.libaums.ErrNo
import com.github.mjdev.libaums.usb.UsbCommunication.Companion.TRANSFER_TIMEOUT
import java.io.IOException


internal abstract class AndroidUsbCommunication(private val deviceConnection: UsbDeviceConnection, private val usbInterface: UsbInterface, private val outEndpoint: UsbEndpoint, private val inEndpoint: UsbEndpoint) : UsbCommunication {

    private var isNativeInited: Boolean = false

    init {
        try {
            System.loadLibrary("usb-lib")
            isNativeInited = true
        } catch (e: UnsatisfiedLinkError) {
            isNativeInited = false
            Log.e(TAG, "could not load usb-lib", e)
        }

    }

    override fun controlTransfer(requestType: Int, request: Int, value: Int, index: Int, buffer: ByteArray, length: Int): Int {
        return deviceConnection.controlTransfer(requestType, request, value, index, buffer, length, TRANSFER_TIMEOUT)
    }

    override fun resetRecovery() {
        if (isNativeInited) {
            Log.w(TAG, "Native reset")
            val result = resetUsbDeviceNative(deviceConnection.fileDescriptor)
            Log.w(TAG, "ioctl returned $result; errno ${ErrNo.errno} ${ErrNo.errstr}; ignoring")
            Thread.sleep(2000)
        } else {
            bulkOnlyMassStorageReset()
            Thread.sleep(2000)
            clearFeatureHalt(inEndpoint)
            Thread.sleep(2000)
            clearFeatureHalt(outEndpoint)
            Thread.sleep(2000)
        }
    }

    override fun bulkOnlyMassStorageReset() {
        Log.w(TAG, "sending bulk only mass storage request")
        val bArr = ByteArray(2)
        // REQUEST_BULK_ONLY_MASS_STORAGE_RESET = 255
        // REQUEST_TYPE_BULK_ONLY_MASS_STORAGE_RESET = 33
        val transferred: Int = controlTransfer(33, 255, 0, usbInterface.id, bArr, 0)
        if (transferred == -1) {
            throw IOException("bulk only mass storage reset failed!")
        }
    }

    override fun clearFeatureHalt(endpoint: UsbEndpoint) {
        Log.w(TAG, "sending clear feature halt")
        val bArr = ByteArray(2)
        val address: Int = endpoint.address
        // REQUEST_CLEAR_FEATURE = 1
        // REQUEST_TYPE_CLEAR_FEATURE = 2
        val transferred: Int = controlTransfer(2, 1, 0, address, bArr, 0)
        if (transferred == -1) {
            throw IOException("bulk only mass storage reset failed!")
        }
    }

    private external fun resetUsbDeviceNative(fd: Int): Boolean

    companion object {
        private val TAG = AndroidUsbCommunication::class.java.simpleName
    }
}
