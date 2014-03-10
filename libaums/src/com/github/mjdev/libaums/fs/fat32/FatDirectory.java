package com.github.mjdev.libaums.fs.fat32;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
	private FatDirectory parent;
	private FatLfnDirectoryEntry entry;
	
	private String volumeLabel;
	
	private FatDirectory(BlockDeviceDriver blockDevice, FAT fat,
			Fat32BootSector bootSector, FatDirectory parent) {
		this.blockDevice = blockDevice;
		this.fat = fat;
		this.bootSector = bootSector;
		this.parent = parent;
		entries = new ArrayList<FatLfnDirectoryEntry>();
		lfnMap = new HashMap<String, FatLfnDirectoryEntry>();
		shortNameMap = new HashMap<ShortName, FatDirectoryEntry>();
	}
	
	/* package */ static FatDirectory create(FatLfnDirectoryEntry entry, BlockDeviceDriver blockDevice, FAT fat, Fat32BootSector bootSector, FatDirectory parent) throws IOException {
		FatDirectory result = new FatDirectory(blockDevice, fat, bootSector, parent);
		result.entry = entry;
		return result;
	}

	/* package */ static FatDirectory readRoot(BlockDeviceDriver blockDevice, FAT fat, Fat32BootSector bootSector) throws IOException {
		FatDirectory result = new FatDirectory(blockDevice, fat, bootSector, null);
		result.chain = new ClusterChain(bootSector.getRootDirStartCluster(), blockDevice, fat, bootSector);
		result.init();
		return result;
	}
	
	private void init() throws IOException {
		if(chain == null) {
			chain = new ClusterChain(entry.getStartCluster(), blockDevice, fat, bootSector);
		}

		if(entries.size() == 0)
			readEntries();
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
			addEntry(lfnEntry, e);
			list.clear();
		}
	}
	
	private void addEntry(FatLfnDirectoryEntry lfnEntry, FatDirectoryEntry entry) {
		entries.add(lfnEntry);
		lfnMap.put(lfnEntry.getName().toLowerCase(Locale.getDefault()), lfnEntry);
		shortNameMap.put(entry.getShortName(), entry);
	}
	
	/* package */  void removeEntry(FatLfnDirectoryEntry lfnEntry) {
		entries.remove(lfnEntry);
		lfnMap.remove(lfnEntry.getName().toLowerCase(Locale.getDefault()));
		shortNameMap.remove(lfnEntry.getActualEntry().getShortName());
	}
	
	/* package */ void renameEntry(FatLfnDirectoryEntry lfnEntry, String newName) throws IOException {
		if(lfnEntry.getName().equals(newName)) return;
		
		removeEntry(lfnEntry);
		lfnEntry.setName(newName, ShortNameGenerator.generateShortName(newName, shortNameMap.keySet()));
		addEntry(lfnEntry, lfnEntry.getActualEntry());
		write();
	}
	
	/* package */ void write() throws IOException {
		init();
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

	@Override
	public FatFile createFile(String name) throws IOException {
		if(lfnMap.containsKey(name.toLowerCase(Locale.getDefault()))) throw new IOException("Item already exists!");
		
		ShortName shortName = ShortNameGenerator.generateShortName(name, shortNameMap.keySet());
		
		FatLfnDirectoryEntry entry = FatLfnDirectoryEntry.createNew(name, shortName);
		long newStartCluster = fat.alloc(new Long[0], 1)[0];
		entry.setStartCluster(newStartCluster);
		
		Log.d(TAG, "adding entry: " + entry + " with short name: " + shortName);
		addEntry(entry, entry.getActualEntry());
		write();
		
		return FatFile.create(entry, blockDevice, fat, bootSector, this);
	}
	
	@Override
	public FatDirectory createDirectory(String name) throws IOException {
		if(lfnMap.containsKey(name.toLowerCase(Locale.getDefault()))) throw new IOException("Item already exists!");

		ShortName shortName = ShortNameGenerator.generateShortName(name, shortNameMap.keySet());
		
		FatLfnDirectoryEntry entry = FatLfnDirectoryEntry.createNew(name, shortName);
		entry.setDirectory();
		long newStartCluster = fat.alloc(new Long[0], 1)[0];
		entry.setStartCluster(newStartCluster);
		
		Log.d(TAG, "adding entry: " + entry + " with short name: " + shortName);
		addEntry(entry, entry.getActualEntry());
		write();
		
		FatDirectory result = FatDirectory.create(entry, blockDevice, fat, bootSector, this);
		
		// first create the dot entry which points to the dir just created
		FatLfnDirectoryEntry dotEntry = FatLfnDirectoryEntry.createNew(null, new ShortName(".", ""));
		dotEntry.setDirectory();
		dotEntry.setStartCluster(newStartCluster);
		FatLfnDirectoryEntry.copyDateTime(entry, dotEntry);
		result.addEntry(dotEntry, dotEntry.getActualEntry());
		
		// Second the dotdot entry which points to the parent directory (this)
		// if parent is the root dir then set start cluster to zero
		FatLfnDirectoryEntry dotDotEntry = FatLfnDirectoryEntry.createNew(null, new ShortName("..", ""));
		dotDotEntry.setDirectory();
		dotDotEntry.setStartCluster(isRoot() ? 0 : entry.getStartCluster());
		FatLfnDirectoryEntry.copyDateTime(entry, dotDotEntry);
		result.addEntry(dotDotEntry, dotDotEntry.getActualEntry());
		
		result.write();
		
		return result;
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
	public void setName(String newName) throws IOException {
		if(isRoot()) throw new IllegalStateException("Cannot rename root dir!");
		parent.renameEntry(entry, newName);
	}

	@Override
	public UsbFile getParent() {
		return parent;
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
				list[i] = FatDirectory.create(entry, blockDevice, fat, bootSector, this);
			} else {
				list[i] = FatFile.create(entry, blockDevice, fat, bootSector, this);
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

	@Override
	public void flush() throws IOException {
		throw new UnsupportedOperationException("This is a directory!");
	}

	@Override
	public void close() throws IOException {
		throw new UnsupportedOperationException("This is a directory!");
	}

	@Override
	public void delete() throws IOException {
		if(isRoot()) throw new IllegalStateException("Root dir cannot be deleted!");
		
		init();
		parent.removeEntry(entry);
		parent.write();
		chain.setLength(0);
	}
}
