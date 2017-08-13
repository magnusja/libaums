package com.github.mjdev.libaums.fs;

import android.support.annotation.Nullable;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.partition.PartitionTableEntry;

import java.io.IOException;

/**
 * Created by magnusja on 28/02/17.
 */

public interface FileSystemCreator {
    @Nullable FileSystem read(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException;
}
