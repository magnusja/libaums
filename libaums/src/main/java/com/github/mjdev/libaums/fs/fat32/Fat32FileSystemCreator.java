package com.github.mjdev.libaums.fs.fat32;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.FileSystemCreator;
import com.github.mjdev.libaums.fs.fat32.Fat32FileSystem;
import com.github.mjdev.libaums.partition.PartitionTableEntry;
import com.github.mjdev.libaums.partition.PartitionTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by magnusja on 28/02/17.
 */

public class Fat32FileSystemCreator implements FileSystemCreator {

    @Override
    public FileSystem read(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException {
        return Fat32FileSystem.read(blockDevice);
    }
}
