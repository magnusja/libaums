package com.github.mjdev.libaums.driver.scsi.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ScsiInquiryResponse {
	
	private byte peripheralQualifier;
	private byte peripheralDeviceType;
	boolean removableMedia;
	byte spcVersion;
	byte responseDataFormat;
	
	private ScsiInquiryResponse() {
		
	}
	
	public static ScsiInquiryResponse read(ByteBuffer buffer) {
		ScsiInquiryResponse response = new ScsiInquiryResponse();
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		byte b = buffer.get();
		response.peripheralQualifier = (byte) (b & (byte) 0xe0);
		response.peripheralDeviceType = (byte) (b & (byte) 0x1f);
		response.removableMedia = buffer.get() == 0x80;
		response.spcVersion = buffer.get();
		response.responseDataFormat = (byte) (buffer.get() & (byte) 0x7);
		return response;
	}

	public byte getPeripheralQualifier() {
		return peripheralQualifier;
	}

	public byte getPeripheralDeviceType() {
		return peripheralDeviceType;
	}

	public boolean isRemovableMedia() {
		return removableMedia;
	}

	public byte getSpcVersion() {
		return spcVersion;
	}

	public byte getResponseDataFormat() {
		return responseDataFormat;
	}

	@Override
	public String toString() {
		return "ScsiInquiryResponse [peripheralQualifier="
				+ peripheralQualifier + ", peripheralDeviceType="
				+ peripheralDeviceType + ", removableMedia=" + removableMedia
				+ ", spcVersion=" + spcVersion + ", responseDataFormat="
				+ responseDataFormat + "]";
	}
}
