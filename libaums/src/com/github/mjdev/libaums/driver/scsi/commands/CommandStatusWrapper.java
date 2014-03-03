package com.github.mjdev.libaums.driver.scsi.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

public class CommandStatusWrapper {
	
	public static final int COMMAND_PASSED = 0;
	public static final int COMMAND_FAILED = 1;
	public static final int PAHSE_ERROR = 2;
	
	public static final int SIZE = 13;
	
	private static final String TAG = CommandStatusWrapper.class.getSimpleName();
	
	private static final int D_CSW_SIGNATURE = 0x53425355;
	
	private int dCswSignature;
	private int dCswTag;
	private int dCswDataResidue;
	private byte bCswStatus;
	
	public static CommandStatusWrapper read(ByteBuffer buffer) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		CommandStatusWrapper csw = new CommandStatusWrapper();
		csw.dCswSignature = buffer.getInt();
		if(csw.dCswSignature != D_CSW_SIGNATURE) {
			Log.e(TAG, "unexpected dCSWSignature");
		}
		csw.dCswTag = buffer.getInt();
		csw.dCswDataResidue = buffer.getInt();
		csw.bCswStatus = buffer.get();
		return csw;
	}

	public int getdCswTag() {
		return dCswTag;
	}

	public int getdCswDataResidue() {
		return dCswDataResidue;
	}

	public byte getbCswStatus() {
		return bCswStatus;
	}
}
