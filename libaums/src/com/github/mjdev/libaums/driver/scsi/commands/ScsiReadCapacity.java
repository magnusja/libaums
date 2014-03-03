package com.github.mjdev.libaums.driver.scsi.commands;

import java.nio.ByteBuffer;

public class ScsiReadCapacity extends CommandBlockWrapper {
	
	private static final int RESPONSE_LENGTH = 0x8;
	private static final byte LENGTH = 0x10;
	private static final byte OPCODE = 0x25;

	public ScsiReadCapacity() {
		super(RESPONSE_LENGTH, Direction.IN, (byte) 0, LENGTH);
	}
	
	@Override
	public void serialize(ByteBuffer buffer) {
		super.serialize(buffer);
		buffer.put(OPCODE);
	}

}
