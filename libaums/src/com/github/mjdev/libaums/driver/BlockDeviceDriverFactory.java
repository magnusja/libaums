package com.github.mjdev.libaums.driver;

import com.github.mjdev.libaums.UsbCommunication;
import com.github.mjdev.libaums.driver.scsi.ScsiBlockDevice;

public class BlockDeviceDriverFactory {
	public static BlockDeviceDriver createBlockDevice(UsbCommunication usbCommunication) {
		// we currently only support scsi transparent command set
		return new ScsiBlockDevice(usbCommunication);
	}
}
