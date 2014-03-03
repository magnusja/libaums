package com.github.mjdev.libaums.partition;

public class PartitionTableEntry {
	
	byte partitionType;
	int logicalBlockAddress;
	int totalNumberOfSectors;
	
	public PartitionTableEntry(byte partitionType, int logicalBlockAddress,
			int totalNumberOfSectors) {
		this.partitionType = partitionType;
		this.logicalBlockAddress = logicalBlockAddress;
		this.totalNumberOfSectors = totalNumberOfSectors;
	}

	public byte getPartitionType() {
		return partitionType;
	}

	public int getLogicalBlockAddress() {
		return logicalBlockAddress;
	}

	public int getTotalNumberOfSectors() {
		return totalNumberOfSectors;
	}
}
