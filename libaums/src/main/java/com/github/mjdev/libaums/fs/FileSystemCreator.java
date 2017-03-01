package com.github.mjdev.libaums.fs;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.partition.PartitionTableEntry;

import java.io.IOException;

/**
 * Created by magnusja on 28/02/17.
 */

public interface FileSystemCreator {
    FileSystem read(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException;
}
