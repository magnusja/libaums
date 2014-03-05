package com.github.mjdev.libaums.fs.fat32;

import java.nio.ByteBuffer;

public class ShortName {
	
	private ByteBuffer data;
	
	private ShortName(ByteBuffer data) {
		this.data = data;
	}
	
	public static ShortName parse(ByteBuffer data) {
		return new ShortName(data);
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
}
