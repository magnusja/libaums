package com.github.mjdev.libaums.fs.fat32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FatDirectoryEntry {
	
    public final static int SIZE = 32;

    private static final int ATTR_OFF = 0x0b;
    private static final int FILE_SIZE_OFF = 0x1c;
    private static final int MSB_CLUSTER_OFF = 0x14;
    private static final int LSB_CLUSTER_OFF = 0x1a;
    
    private static final int FLAG_READONLY = 0x01;
    private static final int FLAG_HIDDEN = 0x02;
    private static final int FLAG_SYSTEM = 0x04;
    private static final int FLAG_VOLUME_ID = 0x08;
    private static final int FLAG_DIRECTORY = 0x10;
    private static final int FLAG_ARCHIVE = 0x20;

    public static final int ENTRY_DELETED = 0xe5;
    
    ByteBuffer data;
    
    private FatDirectoryEntry(ByteBuffer data) {
    	this.data = data;
    	data.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    public static FatDirectoryEntry read(ByteBuffer data) {
    	byte[] buffer = new byte[SIZE];
    	
    	if(data.get(data.position()) == 0) return null;
    	
    	data.get(buffer);
    	
    	return new FatDirectoryEntry(ByteBuffer.wrap(buffer));
    }
    
    private int getFlags() {
    	return data.get(ATTR_OFF);
    }
    
    private boolean isFlagSet(int flag) {
    	return (getFlags() & flag) != 0;
    }

	public boolean isLfnEntry() {
		return isHidden() && isVolume() && isReadOnly() && isSystem();
	}
	
	public boolean isDirectory() {
        return ((getFlags() & (FLAG_DIRECTORY | FLAG_VOLUME_ID)) == FLAG_DIRECTORY);
	}
	
	public boolean isVolumeLabel() {
        if (isLfnEntry()) return false;
        else return ((getFlags() & (FLAG_DIRECTORY | FLAG_VOLUME_ID)) == FLAG_VOLUME_ID);
	}
	
	public boolean isSystem() {
		return isFlagSet(FLAG_SYSTEM);
	}
	
	public boolean isHidden() {
		return isFlagSet(FLAG_HIDDEN);		
	}
	
	public boolean isArchive() {
		return isFlagSet(FLAG_ARCHIVE);		
	}
	
	public boolean isReadOnly() {
		return isFlagSet(FLAG_READONLY);		
	}
	
	public boolean isVolume() {
		return isFlagSet(FLAG_VOLUME_ID);		
	}
	
	public boolean isDeleted() {
		return (data.get(0) & 0xFF) == ENTRY_DELETED;
	}
	
	public String getShortName() {
		if(data.get(0) == 0)
			return null;
		else {
			return parseShortName();
		}
	}
	
	public String parseShortName() {
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
		
		return new String(name).trim() + "." + new String(ext).trim();
	}
	
	public void extractLfnPart(StringBuilder builder) {
		final char[] name = new char[13];
		name[0] = (char) data.getShort(1);
		name[1] = (char) data.getShort(3);
		name[2] = (char) data.getShort(5);
		name[3] = (char) data.getShort(7);
		name[4] = (char) data.getShort(9);
		name[5] = (char) data.getShort(14);
		name[6] = (char) data.getShort(16);
		name[7] = (char) data.getShort(18);
		name[8] = (char) data.getShort(20);
		name[9] = (char) data.getShort(22);
		name[10] = (char) data.getShort(24);
		name[11] = (char) data.getShort(28);
		name[12] = (char) data.getShort(30);
		
		int len = 0;
		while(len < 13 && name[len] != '\0') len++;

		builder.append(name, 0, len);
	}
	
	public String getVolumeLabel() {
		StringBuilder builder = new StringBuilder();
		
		for(int i = 0; i < 11; i++) {
			byte b = data.get(i);
			if(b == 0) break;
			builder.append((char) b);
		}
		
		return builder.toString();
	}
	
	public long getStartCluster() {
		final int unsignedMsb1 = data.get(MSB_CLUSTER_OFF) & 0xFF;
		final int unsignedMsb2 = data.get(MSB_CLUSTER_OFF + 1) & 0xFF;
		final int unsignedLsb1 = data.get(LSB_CLUSTER_OFF) & 0xFF;
		final int unsignedLsb2 = data.get(LSB_CLUSTER_OFF + 1) & 0xFF;
		return (((unsignedMsb2 << 8) | unsignedMsb1) << 16) | (((unsignedLsb2 << 8) | unsignedLsb1));
	}
	
	public long getFileSize() {
		return data.getInt(FILE_SIZE_OFF);
	}
	
}
