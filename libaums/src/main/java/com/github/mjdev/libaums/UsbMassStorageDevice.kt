/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.github.mjdev.libaums

import java.io.IOException
import java.util.ArrayList

import android.annotation.TargetApi
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.driver.BlockDeviceDriverFactory
import com.github.mjdev.libaums.partition.Partition
import com.github.mjdev.libaums.partition.PartitionTable
import com.github.mjdev.libaums.partition.PartitionTableEntry
import com.github.mjdev.libaums.partition.PartitionTableFactory
import com.github.mjdev.libaums.usb.UsbCommunication
import com.github.mjdev.libaums.usb.UsbCommunicationFactory

/**
 * Class representing a connected USB mass storage device. You can enumerate
 * through all connected mass storage devices via
 * [.getMassStorageDevices]. This method only returns supported
 * devices or if no device is connected an empty array.
 *
 *
 * After choosing a device you have to get the permission for the underlying
 * [android.hardware.usb.UsbDevice]. The underlying
 * [android.hardware.usb.UsbDevice] can be accessed via
 * [.getUsbDevice].
 *
 *
 * After that you need to call [.setupDevice]. This will initialize the
 * mass storage device and read the partitions (
 * [com.github.mjdev.libaums.partition.Partition]).
 *
 *
 * The supported partitions can then be accessed via [.getPartitions]
 * and you can begin to read directories and files.
 *
 * @author mjahnen
 */
class UsbMassStorageDevice
/**
 * Construct a new [com.github.mjdev.libaums.UsbMassStorageDevice].
 * The given parameters have to actually be a mass storage device, this is
 * not checked in the constructor!
 *
 * @param usbManager
 * @param usbDevice
 * @param usbInterface
 * @param inEndpoint
 * @param outEndpoint
 */
private constructor(private val usbManager: UsbManager,
                    /**
                     * This returns the [android.hardware.usb.UsbDevice] which can be used
                     * to request permission for communication.
                     *
                     * @return Underlying [android.hardware.usb.UsbDevice] used for
                     * communication.
                     */
                    val usbDevice: UsbDevice,
                    private val usbInterface: UsbInterface, private val inEndpoint: UsbEndpoint, private val outEndpoint: UsbEndpoint) {
    private var deviceConnection: UsbDeviceConnection? = null

    /**
     * Returns the block device interface for this device.
     *
     * Only use this if you know what you are doing, for a interacting (listing/reading/writing files)
     * with a pen drive this is usually not needed
     *
     * @return The BlockDeviceDriver implementation
     */
    var blockDevice: BlockDeviceDriver? = null
        private set
    private var partitionTable: PartitionTable? = null
    private val partitions = ArrayList<Partition>()
    // TODO this is never used, should we only allow one init() call?
    private var inited = false

    /**
     * Initializes the mass storage device and determines different things like
     * for example the MBR or the file systems for the different partitions.
     *
     * @throws IOException
     * If reading from the physical device fails.
     * @throws IllegalStateException
     * If permission to communicate with the underlying
     * [UsbDevice] is missing.
     * @see .getUsbDevice
     */
    @Throws(IOException::class)
    fun init() {
        if (usbManager.hasPermission(usbDevice))
            setupDevice()
        else
            throw IllegalStateException("Missing permission to access usb device: $usbDevice")

        inited = true

    }

    /**
     * Sets the device up. Claims interface and initiates the device connection.
     * Chooses the right[UsbCommunication]
     * depending on the Android version (
     * [com.github.mjdev.libaums.usb.HoneyCombMr1Communication]
     * or (
     * [com.github.mjdev.libaums.usb.JellyBeanMr2Communication]
     * ).
     *
     *
     * Initializes the [.blockDevice] and reads the partitions.
     *
     * @throws IOException
     * If reading from the physical device fails.
     * @see .init
     */
    @Throws(IOException::class)
    private fun setupDevice() {
        Log.d(TAG, "setup device")
        deviceConnection = usbManager.openDevice(usbDevice)
        if (deviceConnection == null) {
            throw IOException("deviceConnection is null!")
        }

        val claim = deviceConnection!!.claimInterface(usbInterface, true)
        if (!claim) {
            throw IOException("could not claim interface!")
        }

        val communication = UsbCommunicationFactory.createUsbCommunication(deviceConnection!!, outEndpoint, inEndpoint)
        val b = ByteArray(1)
        deviceConnection!!.controlTransfer(161, 254, 0, usbInterface.id, b, 1, 5000)
        Log.i(TAG, "MAX LUN " + b[0].toInt())
        blockDevice = BlockDeviceDriverFactory.createBlockDevice(communication)
        blockDevice!!.init()
        partitionTable = PartitionTableFactory.createPartitionTable(blockDevice!!)
        initPartitions()
    }

    /**
     * Fills [.partitions] with the information received by the
     * [.partitionTable].
     *
     * @throws IOException
     * If reading from the [.blockDevice] fails.
     */
    @Throws(IOException::class)
    private fun initPartitions() {
        val partitionEntrys = partitionTable!!.partitionTableEntries

        for (entry in partitionEntrys) {
            val partition = Partition.createPartition(entry, blockDevice!!)
            if (partition != null) {
                partitions.add(partition)
            }
        }
    }

    /**
     * Releases the [android.hardware.usb.UsbInterface] and closes the
     * [android.hardware.usb.UsbDeviceConnection]. After calling this
     * method no further communication is possible. That means you can not read
     * or write from or to the partitions returned by [.getPartitions].
     */
    fun close() {
        Log.d(TAG, "close device")
        if (deviceConnection == null) return

        val release = deviceConnection!!.releaseInterface(usbInterface)
        if (!release) {
            Log.e(TAG, "could not release interface!")
        }
        deviceConnection!!.close()
        inited = false
    }

    /**
     * Returns the available partitions of the mass storage device. You have to
     * call [.init] before calling this method!
     *
     * @return List of partitions.
     */
    fun getPartitions(): List<Partition> {
        return partitions
    }

    companion object {

        private val TAG = UsbMassStorageDevice::class.java.simpleName

        /**
         * subclass 6 means that the usb mass storage device implements the SCSI
         * transparent command set
         */
        private val INTERFACE_SUBCLASS = 6

        /**
         * protocol 80 means the communication happens only via bulk transfers
         */
        private val INTERFACE_PROTOCOL = 80

        /**
         * This method iterates through all connected USB devices and searches for
         * mass storage devices.
         *
         * @param context
         * Context to get the [UsbManager]
         * @return An array of suitable mass storage devices or an empty array if
         * none could be found.
         */
        @JvmStatic
        fun getMassStorageDevices(context: Context): Array<UsbMassStorageDevice> {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val result = ArrayList<UsbMassStorageDevice>()

            for (device in usbManager.deviceList.values) {
                Log.i(TAG, "found usb device: $device")

                val interfaceCount = device.interfaceCount
                for (i in 0 until interfaceCount) {
                    val usbInterface = device.getInterface(i)
                    Log.i(TAG, "found usb interface: $usbInterface")

                    // we currently only support SCSI transparent command set with
                    // bulk transfers only!
                    if (usbInterface.interfaceClass != UsbConstants.USB_CLASS_MASS_STORAGE
                            || usbInterface.interfaceSubclass != INTERFACE_SUBCLASS
                            || usbInterface.interfaceProtocol != INTERFACE_PROTOCOL) {
                        Log.i(TAG, "device interface not suitable!")
                        continue
                    }

                    // Every mass storage device has exactly two endpoints
                    // One IN and one OUT endpoint
                    val endpointCount = usbInterface.endpointCount
                    if (endpointCount != 2) {
                        Log.w(TAG, "inteface endpoint count != 2")
                    }

                    var outEndpoint: UsbEndpoint? = null
                    var inEndpoint: UsbEndpoint? = null
                    for (j in 0 until endpointCount) {
                        val endpoint = usbInterface.getEndpoint(j)
                        Log.i(TAG, "found usb endpoint: $endpoint")
                        if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
                                outEndpoint = endpoint
                            } else {
                                inEndpoint = endpoint
                            }
                        }
                    }

                    if (outEndpoint == null || inEndpoint == null) {
                        Log.e(TAG, "Not all needed endpoints found!")
                        continue
                    }

                    result.add(UsbMassStorageDevice(usbManager, device, usbInterface, inEndpoint,
                            outEndpoint))

                }
            }

            return result.toTypedArray()
        }
    }
}
