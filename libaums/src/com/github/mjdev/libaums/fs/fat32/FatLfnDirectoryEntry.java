package com.github.mjdev.libaums.fs.fat32;

import java.util.List;

public class FatLfnDirectoryEntry {

	private FatDirectoryEntry actualEntry;
	private String lfnName;
	
	private FatLfnDirectoryEntry(FatDirectoryEntry actualEntry, String lfnName) {
		this.actualEntry = actualEntry; 
		this.lfnName = lfnName; 
	}
	
	public static FatLfnDirectoryEntry read(FatDirectoryEntry actualEntry, List<FatDirectoryEntry> lfnParts) {
		StringBuilder builder = new StringBuilder(13 * lfnParts.size());
		
		if(lfnParts.size() > 0) {
			for(int i = lfnParts.size() - 1; i >= 0; i--) {
				lfnParts.get(i).extractLfnPart(builder);
			}
			
			return new FatLfnDirectoryEntry(actualEntry, builder.toString());
		}
		
		return new FatLfnDirectoryEntry(actualEntry, null);
	}
	
	public String getName() {
		if(lfnName != null) return lfnName;
		return actualEntry.getShortName();
	}
	
	public long getFileSize() {
		return actualEntry.getFileSize();
	}
	
	public long getStartCluster() {
		return actualEntry.getStartCluster();
	}
	
	public boolean isDirectory() {
		return actualEntry.isDirectory();
	}
	
}
