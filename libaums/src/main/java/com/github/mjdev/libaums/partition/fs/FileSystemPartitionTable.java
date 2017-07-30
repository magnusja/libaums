package com.github.mjdev.libaums.partition.fs;

import android.util.Log;

import com.github.mjdev.libaums.partition.PartitionTable;
import com.github.mjdev.libaums.partition.PartitionTableEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a dummy partition table. Sometimes devices do not have an MBR or GPT to save memory.
 * https://stackoverflow.com/questions/38004064/is-it-possible-that-small-sd-cards-are-formatted-without-an-mbr
 * Actual File System is then reevaluated in a later stage in {@link com.github.mjdev.libaums.fs.FileSystemFactory}.
 */
public class FileSystemPartitionTable implements PartitionTable {

    private static final String TAG = FileSystemPartitionTable.class.getSimpleName();

    List<PartitionTableEntry> entries = new ArrayList<>();

    public FileSystemPartitionTable() {
        Log.i(TAG, "Found a device without partition table, yay!");
        // TODO fix fs type and total number of sectors
        entries.add(new PartitionTableEntry(-1, 0, -1));
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public Collection<PartitionTableEntry> getPartitionTableEntries() {
        return entries;
    }
}
