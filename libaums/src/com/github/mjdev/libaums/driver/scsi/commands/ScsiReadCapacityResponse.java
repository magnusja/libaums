package com.github.mjdev.libaums.driver.scsi.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ScsiReadCapacityResponse {
	
	private int logicalBlockAddress;
	private int blockLength;
	
	private ScsiReadCapacityResponse() {
		
	}
	
	public static ScsiReadCapacityResponse read(ByteBuffer buffer) {
		buffer.order(ByteOrder.BIG_ENDIAN);
		ScsiReadCapacityResponse res = new ScsiReadCapacityResponse();
		res.logicalBlockAddress = buffer.getInt();
		res.blockLength = buffer.getInt();
		return res;
	}

	public int getLogicalBlockAddress() {
		return logicalBlockAddress;
	}

	public int getBlockLength() {
		return blockLength;
	}
}
