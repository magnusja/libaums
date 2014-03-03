package com.github.mjdev.libaums;

public interface UsbCommunication {
	public int bulkOutTransfer(byte[] buffer, int length);
	public int bulkOutTransfer(byte[] buffer, int offset, int length);
	public int bulkInTransfer(byte[] buffer, int length);
	public int bulkInTransfer(byte[] buffer, int offset, int length);
}
