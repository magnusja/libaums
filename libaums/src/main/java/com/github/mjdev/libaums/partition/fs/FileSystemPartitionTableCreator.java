package com.github.mjdev.libaums.partition.fs;

import android.support.annotation.Nullable;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.driver.ByteBlockDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.FileSystemFactory;
import com.github.mjdev.libaums.partition.PartitionTable;
import com.github.mjdev.libaums.partition.PartitionTableFactory;

import java.io.IOException;

/**
 * Created by magnusja on 30/07/17.
 */

public class FileSystemPartitionTableCreator implements PartitionTableFactory.PartitionTableCreator {
    @Nullable
    @Override
    public PartitionTable read(BlockDeviceDriver blockDevice) throws IOException {
        try {
            return new FileSystemPartitionTable(blockDevice,
                    FileSystemFactory.createFileSystem(null, new ByteBlockDevice(blockDevice)));
        } catch(FileSystemFactory.UnsupportedFileSystemException e) {
            return null;
        }
    }
}
