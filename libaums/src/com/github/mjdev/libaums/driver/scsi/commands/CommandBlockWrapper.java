package com.github.mjdev.libaums.driver.scsi.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class CommandBlockWrapper {
	
	public enum Direction {
		IN,
		OUT,
		NONE
	}
	
	private static final int D_CBW_SIGNATURE = 0x43425355;
	
	private int dCbwTag;
	private int dCbwDataTransferLength;
	private byte bmCbwFlags;
	private byte bCbwLun;
	private byte bCbwcbLength;
	private Direction direction;
	
	protected CommandBlockWrapper(int transferLength, Direction direction, byte lun, byte cbwcbLength) {
		dCbwDataTransferLength = transferLength;
		this.direction = direction;
		if(direction == Direction.IN)
			bmCbwFlags = (byte) 0x80;
		bCbwLun = lun;
		bCbwcbLength = cbwcbLength;
	}
	
	public void serialize(ByteBuffer buffer) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(D_CBW_SIGNATURE);
		buffer.putInt(dCbwTag);
		buffer.putInt(dCbwDataTransferLength);
		buffer.put(bmCbwFlags);
		buffer.put(bCbwLun);
		buffer.put(bCbwcbLength);
	}

	public int getdCbwTag() {
		return dCbwTag;
	}

	public void setdCbwTag(int dCbwTag) {
		this.dCbwTag = dCbwTag;
	}

	public int getdCbwDataTransferLength() {
		return dCbwDataTransferLength;
	}
	
	public Direction getDirection() {
		return direction;
	}
	
}
