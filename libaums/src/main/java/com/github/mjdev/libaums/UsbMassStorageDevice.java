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

package com.github.mjdev.libaums;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.driver.BlockDeviceDriverFactory;
import com.github.mjdev.libaums.partition.Partition;
import com.github.mjdev.libaums.partition.PartitionTable;
import com.github.mjdev.libaums.partition.PartitionTableEntry;
import com.github.mjdev.libaums.partition.PartitionTableFactory;
import com.github.mjdev.libaums.usb.UsbCommunication;
import com.github.mjdev.libaums.usb.UsbCommunicationFactory;

/**
 * Class representing a connected USB mass storage device. You can enumerate
 * through all connected mass storage devices via
 * {@link #getMassStorageDevices(Context)}. This method only returns supported
 * devices or if no device is connected an empty array.
 * <p>
 * After choosing a device you have to get the permission for the underlying
 * {@link android.hardware.usb.UsbDevice}. The underlying
 * {@link android.hardware.usb.UsbDevice} can be accessed via
 * {@link #getUsbDevice()}.
 * <p>
 * After that you need to call {@link #setupDevice()}. This will initialize the
 * mass storage device and read the partitions (
 * {@link com.github.mjdev.libaums.partition.Partition}).
 * <p>
 * The supported partitions can then be accessed via {@link #getPartitions()}
 * and you can begin to read directories and files.
 * 
 * @author mjahnen
 * 
 */
public class UsbMassStorageDevice {

	private static final String TAG = UsbMassStorageDevice.class.getSimpleName();

	/**
	 * subclass 6 means that the usb mass storage device implements the SCSI
	 * transparent command set
	 */
	private static final int INTERFACE_SUBCLASS = 6;

	/**
	 * protocol 80 means the communication happens only via bulk transfers
	 */
	private static final int INTERFACE_PROTOCOL = 80;

	private UsbManager usbManager;
	private UsbDeviceConnection deviceConnection;
	private UsbDevice usbDevice;
	private UsbInterface usbInterface;
	private UsbEndpoint inEndpoint;
	private UsbEndpoint outEndpoint;

	private BlockDeviceDriver blockDevice;
	private PartitionTable partitionTable;
	private List<Partition> partitions = new ArrayList<Partition>();
	// TODO this is never used, should we only allow one init() call?
	private boolean inited = false;

	/**
	 * Construct a new {@link com.github.mjdev.libaums.UsbMassStorageDevice}.
	 * The given parameters have to actually be a mass storage device, this is
	 * not checked in the constructor!
	 * 
	 * @param usbManager
	 * @param usbDevice
	 * @param usbInterface
	 * @param inEndpoint
	 * @param outEndpoint
	 */
	private UsbMassStorageDevice(UsbManager usbManager, UsbDevice usbDevice,
			UsbInterface usbInterface, UsbEndpoint inEndpoint, UsbEndpoint outEndpoint) {
		this.usbManager = usbManager;
		this.usbDevice = usbDevice;
		this.usbInterface = usbInterface;
		this.inEndpoint = inEndpoint;
		this.outEndpoint = outEndpoint;
	}

	/**
	 * This method iterates through all connected USB devices and searches for
	 * mass storage devices.
	 * 
	 * @param context
	 *            Context to get the {@link UsbManager}
	 * @return An array of suitable mass storage devices or an empty array if
	 *         none could be found.
	 */
	public static UsbMassStorageDevice[] getMassStorageDevices(Context context) {
		UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		ArrayList<UsbMassStorageDevice> result = new ArrayList<UsbMassStorageDevice>();

		for (UsbDevice device : usbManager.getDeviceList().values()) {
			Log.i(TAG, "found usb device: " + device);

			int interfaceCount = device.getInterfaceCount();
			for (int i = 0; i < interfaceCount; i++) {
				UsbInterface usbInterface = device.getInterface(i);
				Log.i(TAG, "found usb interface: " + usbInterface);

				// we currently only support SCSI transparent command set with
				// bulk transfers only!
				if (usbInterface.getInterfaceClass() != UsbConstants.USB_CLASS_MASS_STORAGE
						|| usbInterface.getInterfaceSubclass() != INTERFACE_SUBCLASS
						|| usbInterface.getInterfaceProtocol() != INTERFACE_PROTOCOL) {
					Log.i(TAG, "device interface not suitable!");
					continue;
				}

				// Every mass storage device has exactly two endpoints
				// One IN and one OUT endpoint
				int endpointCount = usbInterface.getEndpointCount();
				if (endpointCount != 2) {
					Log.w(TAG, "inteface endpoint count != 2");
				}

				UsbEndpoint outEndpoint = null;
				UsbEndpoint inEndpoint = null;
				for (int j = 0; j < endpointCount; j++) {
					UsbEndpoint endpoint = usbInterface.getEndpoint(j);
					Log.i(TAG, "found usb endpoint: " + endpoint);
					if(endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
						if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
						    outEndpoint = endpoint;
						} else {
						    inEndpoint = endpoint;
						}
					}
				}

				if (outEndpoint == null || inEndpoint == null) {
					Log.e(TAG, "Not all needed endpoints found!");
					continue;
				}

				result.add(new UsbMassStorageDevice(usbManager, device, usbInterface, inEndpoint,
						outEndpoint));

			}
		}

		return result.toArray(new UsbMassStorageDevice[0]);
	}

	/**
	 * Initializes the mass storage device and determines different things like
	 * for example the MBR or the file systems for the different partitions.
	 * 
	 * @throws IOException
	 *             If reading from the physical device fails.
	 * @throws IllegalStateException
	 *             If permission to communicate with the underlying
	 *             {@link UsbDevice} is missing.
	 * @see #getUsbDevice()
	 */
	public void init() throws IOException {
		if (usbManager.hasPermission(usbDevice))
			setupDevice();
		else
			throw new IllegalStateException("Missing permission to access usb device: " + usbDevice);

		inited = true;

	}

	/**
	 * Sets the device up. Claims interface and initiates the device connection.
	 * Chooses the right{@link UsbCommunication}
	 * depending on the Android version (
	 * {@link com.github.mjdev.libaums.usb.HoneyCombMr1Communication}
	 * or (
	 * {@link com.github.mjdev.libaums.usb.JellyBeanMr2Communication}
	 * ).
	 * <p>
	 * Initializes the {@link #blockDevice} and reads the partitions.
	 * 
	 * @throws IOException
	 *             If reading from the physical device fails.
	 * @see #init()
	 */
	private void setupDevice() throws IOException {
		Log.d(TAG, "setup device");
		deviceConnection = usbManager.openDevice(usbDevice);
		if (deviceConnection == null) {
			throw new IOException("deviceConnection is null!");
		}

		boolean claim = deviceConnection.claimInterface(usbInterface, true);
		if (!claim) {
			throw new IOException("could not claim interface!");
		}

		UsbCommunication communication = UsbCommunicationFactory.createUsbCommunication(deviceConnection, outEndpoint, inEndpoint);
		byte[] b = new byte[1];
		deviceConnection.controlTransfer(0b10100001, 0b11111110, 0, usbInterface.getId(), b, 1, 5000);
		Log.i(TAG, "MAX LUN " + (int)b[0]);
		blockDevice = BlockDeviceDriverFactory.createBlockDevice(communication);
		blockDevice.init();
		partitionTable = PartitionTableFactory.createPartitionTable(blockDevice);
		initPartitions();
	}

	/**
	 * Fills {@link #partitions} with the information received by the
	 * {@link #partitionTable}.
	 * 
	 * @throws IOException
	 *             If reading from the {@link #blockDevice} fails.
	 */
	private void initPartitions() throws IOException {
		Collection<PartitionTableEntry> partitionEntrys = partitionTable.getPartitionTableEntries();

		for (PartitionTableEntry entry : partitionEntrys) {
			Partition partition = Partition.createPartition(entry, blockDevice);
			if (partition != null) {
				partitions.add(partition);
			}
		}
	}

	/**
	 * Releases the {@link android.hardware.usb.UsbInterface} and closes the
	 * {@link android.hardware.usb.UsbDeviceConnection}. After calling this
	 * method no further communication is possible. That means you can not read
	 * or write from or to the partitions returned by {@link #getPartitions()}.
	 */
	public void close() {
		Log.d(TAG, "close device");
		if(deviceConnection == null) return;
		
		boolean release = deviceConnection.releaseInterface(usbInterface);
		if (!release) {
			Log.e(TAG, "could not release interface!");
		}
		deviceConnection.close();
		inited = false;
	}

	/**
	 * Returns the available partitions of the mass storage device. You have to
	 * call {@link #init()} before calling this method!
	 * 
	 * @return List of partitions.
	 */
	public List<Partition> getPartitions() {
		return partitions;
	}

	/**
	 * This returns the {@link android.hardware.usb.UsbDevice} which can be used
	 * to request permission for communication.
	 * 
	 * @return Underlying {@link android.hardware.usb.UsbDevice} used for
	 *         communication.
	 */
	public UsbDevice getUsbDevice() {
		return usbDevice;
	}
}
