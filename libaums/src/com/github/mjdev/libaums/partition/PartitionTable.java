package com.github.mjdev.libaums.partition;

import java.util.Collection;

public interface PartitionTable {
	public int getSize();
	public Collection<PartitionTableEntry> getPartitionTableEntries(); 
}
