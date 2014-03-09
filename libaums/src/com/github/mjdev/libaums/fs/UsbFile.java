package com.github.mjdev.libaums.fs;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface UsbFile {
	public boolean isDirectory();
	public String getName();
	public UsbFile getParent();
	public String[] list() throws IOException;
	public UsbFile[] listFiles() throws IOException;
	public long getLength();
	public void setLength(long newLength) throws IOException;
	public void read(long offset, ByteBuffer destination) throws IOException;
	public void write(long offset, ByteBuffer source) throws IOException;
	public void flush() throws IOException;
	public void close() throws IOException;
}
