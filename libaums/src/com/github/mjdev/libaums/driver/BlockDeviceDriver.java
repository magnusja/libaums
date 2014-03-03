package com.github.mjdev.libaums.driver;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface BlockDeviceDriver {
	public void init();
	public void read(long deviceOffset, ByteBuffer buffer) throws IOException;
	public void write(long deviceOffset, ByteBuffer buffer) throws IOException;
	public int getBlockSize();
}
