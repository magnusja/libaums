package com.github.mjdev.libaums.fs.fat32;

import java.nio.ByteBuffer;
import java.util.List;

/* package */ class FatLfnDirectoryEntry {

	private FatDirectoryEntry actualEntry;
	private String lfnName;
	
	private FatLfnDirectoryEntry() {
		
	}
	
	private FatLfnDirectoryEntry(FatDirectoryEntry actualEntry, String lfnName) {
		this.actualEntry = actualEntry; 
		this.lfnName = lfnName; 
	}
	
	/* package */ static FatLfnDirectoryEntry createNew(String name, ShortName shortName) {
		FatLfnDirectoryEntry result = new FatLfnDirectoryEntry();
		
		result.lfnName = name;
		result.actualEntry = FatDirectoryEntry.createNew();
		result.actualEntry.setShortName(shortName);
		
		return result;
	}
	
	/* package */ static FatLfnDirectoryEntry read(FatDirectoryEntry actualEntry, List<FatDirectoryEntry> lfnParts) {
		StringBuilder builder = new StringBuilder(13 * lfnParts.size());
		
		if(lfnParts.size() > 0) {
			for(int i = lfnParts.size() - 1; i >= 0; i--) {
				lfnParts.get(i).extractLfnPart(builder);
			}
			
			return new FatLfnDirectoryEntry(actualEntry, builder.toString());
		}
		
		return new FatLfnDirectoryEntry(actualEntry, null);
	}
	
	/* package */ void serialize(ByteBuffer buffer) {
		if(lfnName != null) {
			byte checksum = actualEntry.getShortName().calculateCheckSum();
			int entrySize = getEntryCount();
			
			// long filename is stored in reverse order
			int index = entrySize - 2;
			// first  write last entry
			FatDirectoryEntry entry = FatDirectoryEntry.createLfnPart(lfnName, index * 13, checksum, index + 1, true);
			entry.serialize(buffer);
			
			while((index--) > 0) {
				entry = FatDirectoryEntry.createLfnPart(lfnName, index * 13, checksum, index + 1, false);
				entry.serialize(buffer);
			}
		}
		
		actualEntry.serialize(buffer);
	}
	
	/* package */ int getEntryCount() {
		// we always have the actual entry
		int result = 1;
		
		// if long filename exists add needed entries
		if(lfnName != null) {
			int len = lfnName.length();
			result += len / 13;
			if(len % 13 != 0) result++;
		}
		
		return result;
	}
	
	/* package */ String getName() {
		if(lfnName != null) return lfnName;
		return actualEntry.getShortName().getString();
	}
	
	/* package */ long getFileSize() {
		return actualEntry.getFileSize();
	}
	
	/* package */ void setFileSize(long newSize) {
		actualEntry.setFileSize(newSize);
	}
	
	/* package */ long getStartCluster() {
		return actualEntry.getStartCluster();
	}
	
	/* package */ void setStartCluster(long newStartCluster) {
		actualEntry.setStartCluster(newStartCluster);
	}
	
	/* package */ void setLastAccessedTimeToNow() {
		actualEntry.setLastAccessedDateTime(System.currentTimeMillis());
	}
	
	/* package */ void setLastModifiedTimeToNow() {
		actualEntry.setLastModifiedDateTime(System.currentTimeMillis());
	}
	
	/* package */ boolean isDirectory() {
		return actualEntry.isDirectory();
	}
	
	/* package */ void setDirectory() {
		actualEntry.setDirectory();
	}

	/* package */ FatDirectoryEntry getActualEntry() {
		return actualEntry;
	}
	
	/* package */ static void copyDateTime(FatLfnDirectoryEntry from, FatLfnDirectoryEntry to) {
		FatDirectoryEntry actualFrom = from.getActualEntry();
		FatDirectoryEntry actualTo = from.getActualEntry();
		actualTo.setCreatedDateTime(actualFrom.getCreatedDateTime());
		actualTo.setLastAccessedDateTime(actualFrom.getLastAccessedDateTime());
		actualTo.setLastModifiedDateTime(actualFrom.getLastModifiedDateTime());
	}
	
	@Override
	public String toString() {
		return "[FatLfnDirectoryEntry getName()=" + getName() + "]";
	}
}
