package com.github.mjdev.libaums.usb

import android.hardware.usb.*
import android.util.Log
import com.github.mjdev.libaums.ErrNo
import com.github.mjdev.libaums.usb.UsbCommunication.Companion.TRANSFER_TIMEOUT
import java.io.IOException


internal abstract class AndroidUsbCommunication(
        private val usbManager: UsbManager,
        private val usbDevice: UsbDevice,
        private val usbInterface: UsbInterface,
        val outEndpoint: UsbEndpoint,
        val inEndpoint: UsbEndpoint
) : UsbCommunication {

    private var isNativeInited: Boolean = false
    var deviceConnection: UsbDeviceConnection? = null
    private var isClosed = false

    init {
        initNativeLibrary()
        initUsbConnection()
    }

    private fun initUsbConnection() {
        if (isClosed)
            return

        Log.d(TAG, "setup device")
        deviceConnection = usbManager.openDevice(usbDevice)
                ?: throw IOException("deviceConnection is null!")

        val claim = deviceConnection!!.claimInterface(usbInterface, true)
        if (!claim) {
            throw IOException("could not claim interface!")
        }
    }

    private fun initNativeLibrary() {
        try {
            System.loadLibrary("usb-lib")
            isNativeInited = true
        } catch (e: UnsatisfiedLinkError) {
            isNativeInited = false
            Log.e(TAG, "could not load usb-lib", e)
        }
    }

    override fun controlTransfer(requestType: Int, request: Int, value: Int, index: Int, buffer: ByteArray, length: Int): Int {
        return deviceConnection!!.controlTransfer(requestType, request, value, index, buffer, length, TRANSFER_TIMEOUT)
    }

    override fun resetRecovery() = when {
        isNativeInited -> nativeReset()
        else           -> softReset()
    }

    private fun nativeReset() {
        Log.d(TAG, "Performing native reset")
        val result = resetUsbDeviceNative(deviceConnection!!.fileDescriptor)

        Log.d(TAG, "ioctl okay: $result; errno ${ErrNo.errno} ${ErrNo.errstr}")
        if (!result)
            Log.w(TAG, "ioctl failed! USB device will likely require new discovery and permissions")

        Thread.sleep(1000)

        Log.d(TAG, "Closing and reopening USB device")
        closeUsbConnection()
        initUsbConnection()
    }

    private fun softReset() {
        Log.w(TAG, "Native helper library not available, can't perform hard device reset")
        bulkOnlyMassStorageReset()
        Thread.sleep(2000)
        clearFeatureHalt(inEndpoint)
        Thread.sleep(2000)
        clearFeatureHalt(outEndpoint)
        Thread.sleep(2000)
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
        Log.w(TAG, "Clearing halt on endpoint $endpoint (direction ${endpoint.direction})")
        val result = clearHaltNative(deviceConnection!!.fileDescriptor, endpoint.address)
        if (!result) {
            Log.e(TAG, "Clear halt failed: errno ${ErrNo.errno} ${ErrNo.errstr}")
        }
    }

    private fun closeUsbConnection() {
        if (deviceConnection == null)
            return

        val release = deviceConnection!!.releaseInterface(usbInterface)
        if (!release) {
            Log.e(TAG, "could not release interface!")
        }

        deviceConnection!!.close()
    }

    override fun close() {
        Log.d(TAG, "close device")
        closeUsbConnection()
        isClosed = true
    }

    companion object {
        private val TAG = AndroidUsbCommunication::class.java.simpleName
    }

    private external fun resetUsbDeviceNative(fd: Int): Boolean
    private external fun clearHaltNative(fd: Int, endpoint: Int): Boolean

}
