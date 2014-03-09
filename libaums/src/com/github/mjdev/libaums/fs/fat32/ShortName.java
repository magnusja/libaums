package com.github.mjdev.libaums.fs.fat32;


import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/* package */ class ShortName {
	
	private static int SIZE = 11;
	
	private ByteBuffer data;
	
	/* package */ ShortName(String name, String extension) {
		byte[] tmp = new byte[SIZE];
		// fill with spaces
		Arrays.fill(tmp, (byte) 0x20);
		
		int length = Math.min(name.length(), 8);
		
        System.arraycopy(name.getBytes(Charset.forName("ASCII")), 0, tmp, 0, length);
        System.arraycopy(extension.getBytes(Charset.forName("ASCII")), 0, tmp, 8, extension.length());
        
        data = ByteBuffer.wrap(tmp);
	}
	
	private ShortName(ByteBuffer data) {
		this.data = data;
	}
	
	/* package */ static ShortName parse(ByteBuffer data) {
		byte[] tmp = new byte[SIZE];
		data.get(tmp);
		return new ShortName(ByteBuffer.wrap(tmp));
	}
	
	/* package */ String getString() {
		final char[] name = new char[8];
		final char[] extension = new char[3];
		
		for(int i = 0; i < 8; i++) {
			name[i] = (char) (data.get(i) & 0xFF);
		}
		
		if(data.get(0) == 0x05) {
			name[0] = (char) 0xe5;
		}
		
		for(int i = 0; i < 3; i++) {
			extension[i] = (char) (data.get(i + 8) & 0xFF);
		}
		
		String strName = new String(name).trim();
		String strExt =  new String(extension).trim();
		
		return strExt.isEmpty() ? strName : strName + "." + strExt;
	}

	/* package */ void serialize(ByteBuffer buffer) {
		buffer.put(data.array(), 0, SIZE);
	}
	
	/* package */ byte calculateCheckSum() {
		int sum = 0;
		
		for(int i = 0; i < SIZE; i++) {
			sum = ((sum & 1) == 1 ? 0x80 : 0) + ((sum & 0xff) >> 1) + data.get(i);
		}
		
		return (byte) (sum & 0xff);
	}
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof ShortName)) return false;
		
		return Arrays.equals(data.array(), ((ShortName) other).data.array());
	}
	
	@Override
	public String toString() {
		return getString();
	}
}
