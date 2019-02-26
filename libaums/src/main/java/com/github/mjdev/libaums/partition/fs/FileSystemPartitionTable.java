package com.github.mjdev.libaums.partition.fs;

import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.FileSystem;
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

    public FileSystemPartitionTable(BlockDeviceDriver blockDevice, FileSystem fs) {
        Log.i(TAG, "Found a device without partition table, yay!");
        int totalNumberOfSectors = (int) fs.getCapacity() / blockDevice.getBlockSize();
        if (fs.getCapacity() % blockDevice.getBlockSize() != 0) {
            Log.w(TAG, "fs capacity is not multiple of block size");
        }
        entries.add(new PartitionTableEntry(fs.getType(), 0, totalNumberOfSectors));
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public List<PartitionTableEntry> getPartitionTableEntries() {
        return entries;
    }
}
