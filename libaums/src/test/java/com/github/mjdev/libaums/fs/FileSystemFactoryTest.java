package com.github.mjdev.libaums.fs;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.driver.file.FileBlockDeviceDriver;
import com.github.mjdev.libaums.fs.fat32.Fat32FileSystem;
import com.github.mjdev.libaums.partition.PartitionTable;
import com.github.mjdev.libaums.partition.PartitionTableEntry;
import com.github.mjdev.libaums.partition.PartitionTableFactory;
import com.github.mjdev.libaums.partition.PartitionTypes;
import com.github.mjdev.libaums.partition.mbr.MasterBootRecord;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * Created by magnusja on 12/08/17.
 */
public class FileSystemFactoryTest {
    @Test
    public void createFat32FileSystem() throws Exception {
        BlockDeviceDriver blockDevice = new FileBlockDeviceDriver(
                new URL("https://www.dropbox.com/s/w3x12zw6d6lc6x5/mbr_1_partition_hfs%2B.bin?dl=1"));

        PartitionTableEntry entry = new PartitionTableEntry(PartitionTypes.FAT32, 2 * 512, 1337);
        FileSystem fs = FileSystemFactory.createFileSystem(entry, blockDevice);

        assertTrue(fs instanceof Fat32FileSystem);
    }

}