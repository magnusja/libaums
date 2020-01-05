package com.github.mjdev.libaums.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

/**
 * Created by magnusja on 21/12/16.
 */

object UsbCommunicationFactory {

    private val TAG = UsbCommunicationFactory::class.java.simpleName

    @JvmStatic
    var underlyingUsbCommunication = UnderlyingUsbCommunication.DEVICE_CONNECTION_SYNC

    enum class UnderlyingUsbCommunication {
        USB_REQUEST_ASYNC,
        DEVICE_CONNECTION_SYNC
    }

    fun createUsbCommunication(
            usbManager: UsbManager,
            usbDevice: UsbDevice,
            usbInterface: UsbInterface,
            outEndpoint: UsbEndpoint,
            inEndpoint: UsbEndpoint
    ): UsbCommunication {
        return if (underlyingUsbCommunication == UnderlyingUsbCommunication.DEVICE_CONNECTION_SYNC) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                JellyBeanMr2Communication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint)
            } else {
                Log.i(TAG, "using workaround usb communication")
                HoneyCombMr1Communication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint)
            }
        } else {
            UsbRequestCommunication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint)
        }
    }
}
