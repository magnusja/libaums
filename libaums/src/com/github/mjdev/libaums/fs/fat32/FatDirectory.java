package com.github.mjdev.libaums.fs.fat32;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private Map<String, FatLfnDirectoryEntry> lfnMap;
	private Map<ShortName, FatDirectoryEntry> shortNameMap;
	private FatLfnDirectoryEntry entry;
	
	private String volumeLabel;
	
	private FatDirectory(BlockDeviceDriver blockDevice, FAT fat,
			Fat32BootSector bootSector) {
		this.blockDevice = blockDevice;
		this.fat = fat;
		this.bootSector = bootSector;
		entries = new ArrayList<FatLfnDirectoryEntry>();
		lfnMap = new HashMap<String, FatLfnDirectoryEntry>();
		shortNameMap = new HashMap<ShortName, FatDirectoryEntry>();
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
				if(!isRoot()) {
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
			lfnMap.put(lfnEntry.getName(), lfnEntry);
			shortNameMap.put(e.getShortName(), e);
			list.clear();
		}
	}
	
	private void write() throws IOException {
		final boolean writeVolumeLabel = isRoot() && volumeLabel != null;
		// first lookup the total entries needed
		int totalEntryCount = 0;
		for(FatLfnDirectoryEntry entry : entries) {
			totalEntryCount += entry.getEntryCount();
		}
		
		if(writeVolumeLabel)
			totalEntryCount++;
		
		long totalBytes = totalEntryCount * FatDirectoryEntry.SIZE;
		chain.setLength(totalBytes);
		
		ByteBuffer buffer = ByteBuffer.allocate((int)chain.getLength());
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		if(writeVolumeLabel)
			FatDirectoryEntry.createVolumeLabel(volumeLabel).serialize(buffer);
		
		for(FatLfnDirectoryEntry entry : entries) {
			entry.serialize(buffer);
		}
		
		if(totalBytes % bootSector.getBytesPerCluster() != 0) {
			// add dummy entry filled with zeros to mark end of entries
			buffer.put(new byte[32]);
		}
		
		buffer.flip();
		chain.write(0, buffer);
	}
	
	private boolean isRoot() {
		return entry == null;
	}
	
	/* package */ String getVolumeLabel() {
		return volumeLabel;
	}

	public UsbFile createFile(String name) throws IOException {
		ShortName shortName = ShortNameGenerator.generateShortName(name, shortNameMap.keySet());
		
		FatLfnDirectoryEntry entry = FatLfnDirectoryEntry.createNew(name, shortName);
		long newStartCluster = fat.alloc(new Long[0], 1)[0];
		entry.setStartCluster(newStartCluster);
		
		Log.d(TAG, "adding entry: " + entry + " with short name: " + shortName);
		entries.add(entry);
		write();
		
		return FatFile.create(entry, blockDevice, fat, bootSector);
	}
	
	@Override
	public void setLength(long newLength) {
		throw new UnsupportedOperationException("This is a directory!");
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

	@Override
	public void write(long offset, ByteBuffer source) throws IOException {
		throw new UnsupportedOperationException("This is a directory!");
	}
}
