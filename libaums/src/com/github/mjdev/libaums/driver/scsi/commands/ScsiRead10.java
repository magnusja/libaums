package com.github.mjdev.libaums.driver.scsi.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

public class ScsiRead10 extends CommandBlockWrapper {
	
	private static final String TAG = ScsiRead10.class.getSimpleName();
	private static final byte LENGTH = 0x10;
	private static final byte OPCODE = 0x28;

	private int blockAddress;
	private int transferBytes;
	private int blockSize;
	private short transferBlocks;
	
	// TODO check if we can read 1 sector but with transfer length < blockSize
	// -- works --
	public ScsiRead10(int blockAddress, int transferBytes, int blockSize) {
		super(transferBytes, Direction.IN, (byte) 0, LENGTH);
		this.blockAddress = blockAddress;
		this.transferBytes = transferBytes;
		this.blockSize = blockSize;
		short transferBlocks = (short) (transferBytes / blockSize);
		if(transferBytes % blockSize != 0) {
			Log.w(TAG, "transfer bytes is not a multiple of block size");
			if(transferBlocks == 0) transferBlocks = 1;
		}
		this.transferBlocks = transferBlocks;
	}
	
	@Override
	public void serialize(ByteBuffer buffer) {
		super.serialize(buffer);
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.put(OPCODE);
		buffer.put((byte) 0);
		buffer.putInt(blockAddress);
		buffer.put((byte) 0);
		buffer.putShort(transferBlocks);
	}

	@Override
	public String toString() {
		return "ScsiRead10 [blockAddress=" + blockAddress + ", transferBytes="
				+ transferBytes + ", blockSize=" + blockSize
				+ ", transferBlocks=" + transferBlocks
				+ ", getdCbwDataTransferLength()="
				+ getdCbwDataTransferLength() + "]";
	}

}
