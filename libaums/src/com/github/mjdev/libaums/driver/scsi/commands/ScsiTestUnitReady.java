package com.github.mjdev.libaums.driver.scsi.commands;

import java.nio.ByteBuffer;

public class ScsiTestUnitReady extends CommandBlockWrapper {

	private static final byte LENGTH = 0x6;
	private static final byte OPCODE = 0x12;
	
	public ScsiTestUnitReady() {
		super(0, Direction.NONE, (byte) 0, LENGTH);
	}
	
	@Override
	public void serialize(ByteBuffer buffer) {
		super.serialize(buffer);
		buffer.put(OPCODE);
	}

}
