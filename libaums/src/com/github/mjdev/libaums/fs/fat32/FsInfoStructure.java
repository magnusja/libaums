package com.github.mjdev.libaums.fs.fat32;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;

public class FsInfoStructure {
	
	private static int LEAD_SIGNATURE_OFF = 0;
	private static int STRUCT_SIGNATURE_OFF = 484;
	private static int TRAIL_SIGNATURE_OFF = 508;
	private static int FREE_COUNT_OFF = 488;
	private static int NEXT_FREE_OFFSET = 492;
	
	private static int LEAD_SIGNATURE = 0x41615252;
	private static int STRUCT_SIGNATURE = 0x61417272;
	private static int TRAIL_SIGNATURE = 0xAA550000;
	
	private static final String TAG = FsInfoStructure.class.getSimpleName();

	private int offset;
	private BlockDeviceDriver blockDevice;
	private ByteBuffer buffer;
	
	private FsInfoStructure(BlockDeviceDriver blockDevice, int offset) throws IOException {
		buffer = ByteBuffer.allocate(512);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		blockDevice.read(offset, buffer);
		
		if(buffer.getInt(LEAD_SIGNATURE_OFF) != LEAD_SIGNATURE ||
				buffer.getInt(STRUCT_SIGNATURE_OFF) != STRUCT_SIGNATURE ||
				buffer.getInt(TRAIL_SIGNATURE_OFF) != TRAIL_SIGNATURE) {
			Log.e(TAG, "invalid fs info structure!");
		}
	}
	
	public static FsInfoStructure read(BlockDeviceDriver blockDevice, int offset) throws IOException {
		return new FsInfoStructure(blockDevice, offset);
	}
	
	public long getFreeClusterCount() {
		return buffer.getInt(FREE_COUNT_OFF);
	}
	
	public long getLastAllocatedClusterHint() {
		return buffer.getInt(NEXT_FREE_OFFSET);
	}
}
