package com.github.mjdev.libaums.fs.fat16;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.FileSystemCreator;
import com.github.mjdev.libaums.partition.PartitionTableEntry;

import java.io.IOException;

/**
 * Created by magnusja on 28/02/17.
 */

public class Fat16FileSystemCreator implements FileSystemCreator {

    @Override
    public FileSystem read(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException {
        return Fat16FileSystem.read(blockDevice);
    }
}
