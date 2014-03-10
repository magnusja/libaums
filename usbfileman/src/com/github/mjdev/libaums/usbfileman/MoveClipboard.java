package com.github.mjdev.libaums.usbfileman;

import com.github.mjdev.libaums.fs.UsbFile;

public class MoveClipboard {
	
	private static MoveClipboard instance;
	private UsbFile file;
	
	private MoveClipboard() {
		
	}
	
	public static synchronized MoveClipboard getInstance() {
		if(instance == null)
			instance = new MoveClipboard();
		
		return instance;
	}

	public UsbFile getFile() {
		return file;
	}

	public void setFile(UsbFile file) {
		this.file = file;
	}
	
}
