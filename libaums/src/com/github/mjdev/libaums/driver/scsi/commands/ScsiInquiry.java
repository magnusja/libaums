package com.github.mjdev.libaums.driver.scsi.commands;

import java.nio.ByteBuffer;

public class ScsiInquiry extends CommandBlockWrapper {
	
	private static final int RESPONSE_LENGTH = 0x24;
	private static final byte LENGTH = 0x6;
	private static final byte OPCODE = 0x12;
	
	public ScsiInquiry() {
		super(RESPONSE_LENGTH, Direction.IN, (byte) 0, LENGTH);
	}
	
	@Override
	public void serialize(ByteBuffer buffer) {
		super.serialize(buffer);
		buffer.put(OPCODE);
		buffer.put((byte) 0);
		buffer.put((byte) 0);
		buffer.put((byte) 0);
		buffer.put((byte) LENGTH);
	}

}
