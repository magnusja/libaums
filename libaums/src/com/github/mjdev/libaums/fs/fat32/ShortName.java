package com.github.mjdev.libaums.fs.fat32;

import java.nio.ByteBuffer;

public class ShortName {
	
	private ByteBuffer data;
	
	private ShortName(ByteBuffer data) {
		this.data = data;
	}
	
	public static ShortName parse(ByteBuffer data) {
		byte[] tmp = new byte[13];
		data.get(tmp);
		return new ShortName(ByteBuffer.wrap(tmp));
	}
	
	public String getString() {
		final char[] name = new char[8];
		final char[] ext = new char[3];
		
		for(int i = 0; i < 8; i++) {
			name[i] = (char) (data.get(i) & 0xFF);
		}
		
		if(data.get(0) == 0x05) {
			name[0] = (char) 0xe5;
		}
		
		for(int i = 0; i < 3; i++) {
			ext[i] = (char) (data.getChar(i + 8) & 0xFF);
		}
		
		String strName = new String(name).trim();
		String strExt =  new String(ext).trim();
		
		return strExt.isEmpty() ? strName : strName + "." + strExt;
	}

	public void serialize(ByteBuffer buffer) {
		buffer.put(data.array(), 0, 13);
	}
	
	public byte calculateCheckSum() {
		int sum = 0;
		
		for(int i = 0; i < 11; i++) {
			sum = ((sum & 1) == 1 ? 0x80 : 0) + ((sum & 0xff) >> 1) + data.get(i);
		}
		
		return (byte) (sum & 0xff);
	}
}
