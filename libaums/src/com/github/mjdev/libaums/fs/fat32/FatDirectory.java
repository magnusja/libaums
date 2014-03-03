package com.github.mjdev.libaums.fs.fat32;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.UsbFile;

public class FatDirectory implements UsbFile {
	
	private static String TAG = FatDirectory.class.getSimpleName();

	private ClusterChain chain;
	private BlockDeviceDriver blockDevice;
	private FAT fat;
	private Fat32BootSector bootSector;
	private List<FatLfnDirectoryEntry> entries;
	private FatLfnDirectoryEntry entry;
	
	private String volumeLabel;
	
	private FatDirectory(BlockDeviceDriver blockDevice, FAT fat,
			Fat32BootSector bootSector) {
		this.blockDevice = blockDevice;
		this.fat = fat;
		this.bootSector = bootSector;
		entries = new ArrayList<FatLfnDirectoryEntry>();
	}
	
	private void init() throws IOException {
		if(chain == null) {
			chain = new ClusterChain(entry.getStartCluster(), blockDevice, fat, bootSector);
		}

		if(entries.size() == 0)
			readEntries();
	}
	
	public static FatDirectory create(FatLfnDirectoryEntry entry, BlockDeviceDriver blockDevice, FAT fat, Fat32BootSector bootSector) throws IOException {
		FatDirectory result = new FatDirectory(blockDevice, fat, bootSector);
		result.entry = entry;
		result.chain = new ClusterChain(entry.getStartCluster(), blockDevice, fat, bootSector);
		return result;
	}

	public static FatDirectory readRoot(BlockDeviceDriver blockDevice, FAT fat, Fat32BootSector bootSector) throws IOException {
		FatDirectory result = new FatDirectory(blockDevice, fat, bootSector);
		result.chain = new ClusterChain(bootSector.getRootDirStartCluster(), blockDevice, fat, bootSector);
		result.init();
		return result;
	}
	
	private void readEntries() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate((int)chain.getLength());
		chain.read(0, buffer);
		ArrayList<FatDirectoryEntry> list = new ArrayList<FatDirectoryEntry>();
		buffer.flip();
		while(buffer.remaining() > 0) {
			FatDirectoryEntry e = FatDirectoryEntry.read(buffer);
			if(e == null) {
				break;
			}
			
			if(e.isLfnEntry()) {
				list.add(e);
				continue;
			}
			
			if(e.isVolumeLabel()) {
				if(entry != null) {
					Log.w(TAG, "volume label in non root dir!");
				}
				volumeLabel = e.getVolumeLabel();
				Log.d(TAG, "volume label: " + volumeLabel);
				continue;
			}
			
			if(e.isDeleted()) {
				list.clear();
				continue;
			}
			
			FatLfnDirectoryEntry lfnEntry = FatLfnDirectoryEntry.read(e, list);
			entries.add(lfnEntry);
			list.clear();
		}
	}
	
	/* package */ String getVolumeLabel() {
		return volumeLabel;
	}

	@Override
	public long getLength() {
		throw new UnsupportedOperationException("This is a directory!");
	}
	
	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public String getName() {
		return entry.getName();
	}

	@Override
	public String[] list() throws IOException {
		init();
		String[] list = new String[entries.size()];
		for(int i = 0; i < entries.size(); i++)
			list[i] = entries.get(i).getName();
		return list;
	}

	@Override
	public UsbFile[] listFiles() throws IOException {
		init();
		UsbFile[] list = new UsbFile[entries.size()];
		for(int i = 0; i < entries.size(); i++) {
			FatLfnDirectoryEntry entry = entries.get(i);
			if(entry.isDirectory()) {
				list[i] = FatDirectory.create(entry, blockDevice, fat, bootSector);
			} else {
				list[i] = FatFile.create(entry, blockDevice, fat, bootSector);
			}
		}
		return list;
	}

	@Override
	public void read(long offset, ByteBuffer destination) throws IOException {
		throw new UnsupportedOperationException("This is a directory!");
	}
}
