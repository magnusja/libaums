package com.github.mjdev.libaums;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.driver.BlockDeviceDriverFactory;
import com.github.mjdev.libaums.partition.Partition;
import com.github.mjdev.libaums.partition.PartitionTable;
import com.github.mjdev.libaums.partition.PartitionTableEntry;
import com.github.mjdev.libaums.partition.PartitionTableFactory;

public class UsbMassStorageDevice {

	// subclass 6 means that the usb mass storage device implements the
	// SCSI transparent command set
	private static final int INTERFACE_SUBCLASS = 6;
	// protocol 80 means the communication happens only via bulk transfers
	private static final int INTERFACE_PROTOCOL = 80;
	
	private static final String TAG = UsbMassStorageDevice.class.getSimpleName();
	
	private UsbManager usbManager;
	private UsbDeviceConnection deviceConnection;
	private UsbDevice usbDevice;
	private UsbInterface usbInterface;
	private UsbEndpoint inEndpoint;
	private UsbEndpoint outEndpoint;
	
	private BlockDeviceDriver blockDevice;
	private PartitionTable partitionTable;
	private List<Partition> partitions = new ArrayList<Partition>();
	
	public UsbMassStorageDevice(UsbManager usbManager, UsbDevice usbDevice,
			UsbInterface usbInterface, UsbEndpoint inEndpoint,
			UsbEndpoint outEndpoint) {
		this.usbManager = usbManager;
		this.usbDevice = usbDevice;
		this.usbInterface = usbInterface;
		this.inEndpoint = inEndpoint;
		this.outEndpoint = outEndpoint;
	}

	public static UsbMassStorageDevice[] getMassStorageDevices(Context context) {
		UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		ArrayList<UsbMassStorageDevice> result = new ArrayList<UsbMassStorageDevice>();
		
		for(UsbDevice device : usbManager.getDeviceList().values()) {
			Log.i(TAG, "found usb device: " + device);
			
			int interfaceCount = device.getInterfaceCount();
			for(int i = 0; i < interfaceCount; i++) {
				UsbInterface usbInterface = device.getInterface(i);
				Log.i(TAG, "found usb interface: " + usbInterface);
				
				// we currently only support SCSI transparent command set with bulk transfers only!
				if(usbInterface.getInterfaceClass() != UsbConstants.USB_CLASS_MASS_STORAGE ||
						usbInterface.getInterfaceSubclass() != INTERFACE_SUBCLASS ||
						usbInterface.getInterfaceProtocol() != INTERFACE_PROTOCOL) {
					Log.i(TAG, "device interface not suitable!");
					continue;
				}
				
				int endpointCount = usbInterface.getEndpointCount();
				if(endpointCount != 2) {
					Log.w(TAG, "inteface endpoint count != 2");
				}
				
				UsbEndpoint outEndpoint = null;
				UsbEndpoint inEndpoint = null;
				for(int j = 0; j < endpointCount; j++) {
					UsbEndpoint endpoint = usbInterface.getEndpoint(j);
					Log.i(TAG, "found usb endpoint: " + endpoint);
					if(endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
						outEndpoint = endpoint;
					} else {
						inEndpoint = endpoint;
					}
				}
				
				if(outEndpoint == null || inEndpoint == null) {
					Log.e(TAG, "Not all needed endpoints found!");
					continue;
				}
				
				result.add(new UsbMassStorageDevice(usbManager, device, usbInterface, inEndpoint, outEndpoint));
				
			}
		}
		
		return result.toArray(new UsbMassStorageDevice[0]);
	}
	
	public void init() throws IOException {
		if(usbManager.hasPermission(usbDevice))
			setupDevice();
		else
			throw new IllegalStateException("Missing permission to access usb device: " + usbDevice);
			
	}
	
	private void setupDevice() throws IOException {
		Log.d(TAG, "setup device");
		deviceConnection = usbManager.openDevice(usbDevice);
		if(deviceConnection == null) {
			Log.e(TAG, "deviceConnetion is null!");
			return;
		}
		
		boolean claim = deviceConnection.claimInterface(usbInterface, true);
		if(!claim) {
			Log.e(TAG, "could not claim interface!");
			return;
		}
		
		blockDevice = BlockDeviceDriverFactory.createBlockDevice(this);
		blockDevice.init();
		partitionTable = PartitionTableFactory.createPartitionTable(blockDevice);
		initPartitions();
	}
	
	private void initPartitions() throws IOException {
		Collection<PartitionTableEntry> partitionEntrys = partitionTable.getPartitionTableEntries();
		
		for(PartitionTableEntry entry : partitionEntrys) {
			Partition partition = Partition.createPartition(entry, blockDevice);
			if(partition != null) {
				partitions.add(partition);
			}
		}
	}
	
	public void close() {
		Log.d(TAG, "close device");
		boolean release = deviceConnection.releaseInterface(usbInterface);
		if(!release) {
			Log.e(TAG, "could not release interface!");
		}
		deviceConnection.close();
	}
	
	// TODO maybe remove this methods and instead give a block device direct access to endpoints?
	public int bulkOutTransfer(byte[] buffer, int length) {
		return deviceConnection.bulkTransfer(outEndpoint, buffer, length, 21000);
	}
	
	public int bulkOutTransfer(byte[] buffer, int offset, int length) {
		return deviceConnection.bulkTransfer(outEndpoint, buffer, offset, length, 21000);
	}
	
	public int bulkInTransfer(byte[] buffer, int length) {
		return deviceConnection.bulkTransfer(inEndpoint, buffer, length, 21000);
	}
	
	public int bulkInTransfer(byte[] buffer, int offset, int length) {
		return deviceConnection.bulkTransfer(inEndpoint, buffer, offset, length, 21000);
	}
	
	public List<Partition> getPartitions() {
		return partitions;
	}

	public UsbDevice getUsbDevice() {
		return usbDevice;
	}
	
	public BlockDeviceDriver getScsiBlockDevice() {
		return blockDevice;
	}
}
