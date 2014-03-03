package com.github.mjdev.libaums.driver;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.driver.scsi.ScsiBlockDevice;

public class BlockDeviceDriverFactory {
	public static BlockDeviceDriver createBlockDevice(UsbMassStorageDevice massStorageDevice) {
		// we currently only support scsi transparent command set
		return new ScsiBlockDevice(massStorageDevice);
	}
}
