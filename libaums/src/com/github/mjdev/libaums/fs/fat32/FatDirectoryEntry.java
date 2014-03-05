package com.github.mjdev.libaums.fs.fat32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

public class FatDirectoryEntry {
	
    public final static int SIZE = 32;

    private static final int ATTR_OFF = 0x0b;
    private static final int FILE_SIZE_OFF = 0x1c;
    private static final int MSB_CLUSTER_OFF = 0x14;
    private static final int LSB_CLUSTER_OFF = 0x1a;
    private static final int CREATED_DATE_OFF = 0x10;
    private static final int CREATED_TIME_OFF = 0x0e;
    private static final int LAST_WRITE_DATE_OFF = 0x18;
    private static final int LAST_WRITE_TIME_OFF = 0x16;
    private static final int LAST_ACCESSED_DATE_OFF = 0x12;
    
    private static final int FLAG_READONLY = 0x01;
    private static final int FLAG_HIDDEN = 0x02;
    private static final int FLAG_SYSTEM = 0x04;
    private static final int FLAG_VOLUME_ID = 0x08;
    private static final int FLAG_DIRECTORY = 0x10;
    private static final int FLAG_ARCHIVE = 0x20;

    public static final int ENTRY_DELETED = 0xe5;
    
    ByteBuffer data;
    private ShortName shortName;
    
    private FatDirectoryEntry(ByteBuffer data) {
    	this.data = data;
    	data.order(ByteOrder.LITTLE_ENDIAN);
    	shortName = ShortName.parse(data);
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
		return getUnsignedInt8(ENTRY_DELETED) == ENTRY_DELETED;
	}
	
	public long getCreatedDateTime() {
		return decodeDateTime(getUnsignedInt16(CREATED_DATE_OFF), getUnsignedInt16(CREATED_TIME_OFF));
	}
	
	public long getLastAccessedDateTime() {
		return decodeDateTime(getUnsignedInt16(LAST_WRITE_DATE_OFF), getUnsignedInt16(LAST_WRITE_TIME_OFF));
	}
	
	public long getLastModifiedDateTime() {
		return decodeDateTime(getUnsignedInt16(LAST_ACCESSED_DATE_OFF), 0);
	}
	
	public String getShortName() {
		if(data.get(0) == 0)
			return null;
		else {
			return shortName.getString();
		}
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
		final int msb = getUnsignedInt16(MSB_CLUSTER_OFF);
		final int lsb = getUnsignedInt16(LSB_CLUSTER_OFF);
		return (msb << 16) | lsb;
	}
	
	public long getFileSize() {
		return getUnsignedInt32(FILE_SIZE_OFF);
	}

	private int getUnsignedInt8(int offset) {
		return data.get(offset) & 0xff;
	}
	
	private int getUnsignedInt16(int offset) {
		final int i1 = data.get(offset) & 0xff;
		final int i2 = data.get(offset + 1) & 0xff;
		return (i2 << 8) | i1;
	}
	
	private int getUnsignedInt32(int offset) {
		final int i1 = data.get(offset) & 0xff;
		final int i2 = data.get(offset + 1) & 0xff;
		final int i3 = data.get(offset + 2) & 0xff;
		final int i4 = data.get(offset + 3) & 0xff;
		return (i4 << 24) | (i3 << 16) | (i2 << 8) | i1;
	}
	
	private static long decodeDateTime(int date, int time) {
		final Calendar calendar = Calendar.getInstance();
		
		calendar.set(Calendar.YEAR, 1980 + (date >> 9));
		calendar.set(Calendar.MONTH, ((date >> 5) & 0x0f) - 1);
		calendar.set(Calendar.DAY_OF_MONTH, date & 0x0f);
		calendar.set(Calendar.HOUR_OF_DAY, time >> 11);
		calendar.set(Calendar.MINUTE, (time >> 5) & 0x3f);
		calendar.set(Calendar.SECOND, (time & 0x1f) * 2);
		
		return calendar.getTimeInMillis();
	}
	
	private static int encodeDate(long timeInMillis) {
		final Calendar calendar = Calendar.getInstance();
		
		calendar.setTimeInMillis(timeInMillis);
		
		return ((calendar.get(Calendar.YEAR) - 1980) << 9) + ((calendar.get(Calendar.MONTH) + 1) << 5) + calendar.get(Calendar.DAY_OF_MONTH);
		
	}
	
	private static int encodeTime(long timeInMillis) {
		final Calendar calendar = Calendar.getInstance();
		
		calendar.setTimeInMillis(timeInMillis);
		
		return (calendar.get(Calendar.HOUR_OF_DAY) << 11) + (calendar.get(Calendar.MINUTE) << 5) + calendar.get(Calendar.SECOND) / 2;
		
	}
}
