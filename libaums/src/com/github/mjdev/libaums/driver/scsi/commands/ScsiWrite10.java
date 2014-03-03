package com.github.mjdev.libaums.driver.scsi.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

public class ScsiWrite10 extends CommandBlockWrapper {
	
	private static final String TAG = ScsiRead10.class.getSimpleName();
	private static final byte LENGTH = 0x10;
	private static final byte OPCODE = 0x2a;

	private int blockAddress;
	private int transferBytes;
	private int blockSize;
	private short transferBlocks;
	
	public ScsiWrite10(int blockAddress, int transferBytes, int blockSize) {
		super(transferBytes, Direction.OUT, (byte) 0, LENGTH);
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
		return "ScsiWrite10 [blockAddress=" + blockAddress + ", transferBytes="
				+ transferBytes + ", blockSize=" + blockSize
				+ ", getdCbwDataTransferLength()="
				+ getdCbwDataTransferLength() + "]";
	}

}
