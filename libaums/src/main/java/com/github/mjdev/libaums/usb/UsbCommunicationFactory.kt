package com.github.mjdev.libaums.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import java.io.IOException
import java.util.*

/**
 * Created by magnusja on 21/12/16.
 */

object UsbCommunicationFactory {
    class NoUsbCommunicationFound : IOException()

    private val TAG = UsbCommunicationFactory::class.java.simpleName

    private val communications = ArrayList<UsbCommunicationCreator>()

    @JvmStatic
    var underlyingUsbCommunication = UnderlyingUsbCommunication.DEVICE_CONNECTION_SYNC

    enum class UnderlyingUsbCommunication {
        USB_REQUEST_ASYNC,
        DEVICE_CONNECTION_SYNC,
        OTHER
    }

    @Synchronized
    @JvmStatic
    fun registerCommunication(creator: UsbCommunicationCreator) {
        communications.add(creator)
    }

    fun createUsbCommunication(
            usbManager: UsbManager,
            usbDevice: UsbDevice,
            usbInterface: UsbInterface,
            outEndpoint: UsbEndpoint,
            inEndpoint: UsbEndpoint
    ): UsbCommunication {
        when(underlyingUsbCommunication) {
            UnderlyingUsbCommunication.DEVICE_CONNECTION_SYNC ->
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    JellyBeanMr2Communication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint)
                } else {
                    Log.i(TAG, "using workaround usb communication")
                    HoneyCombMr1Communication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint)
                }

            UnderlyingUsbCommunication.USB_REQUEST_ASYNC ->
                return UsbRequestCommunication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint)

            UnderlyingUsbCommunication.OTHER ->
                for(creator in communications) {
                    val communication = creator.create(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint)

                    if(communication != null) {
                        return communication
                    }
                }

        }
        throw NoUsbCommunicationFound()
    }
}

interface UsbCommunicationCreator {
    fun create(usbManager: UsbManager,
            usbDevice: UsbDevice,
            usbInterface: UsbInterface,
            outEndpoint: UsbEndpoint,
            inEndpoint: UsbEndpoint): UsbCommunication?
}
