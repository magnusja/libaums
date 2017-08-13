package com.github.mjdev.libaums.partition;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.driver.ByteBlockDevice;
import com.github.mjdev.libaums.driver.file.FileBlockDeviceDriver;
import com.github.mjdev.libaums.partition.fs.FileSystemPartitionTable;
import com.github.mjdev.libaums.partition.mbr.MasterBootRecord;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * Created by magnusja on 12/08/17.
 */
public class PartitionTableFactoryTest {
    @Test
    public void testMbrCreate() throws Exception {
        BlockDeviceDriver blockDevice = new FileBlockDeviceDriver(
                new URL("https://www.dropbox.com/s/w3x12zw6d6lc6x5/mbr_1_partition_hfs%2B.bin?dl=1"));
        blockDevice.init();

        PartitionTable table = PartitionTableFactory.createPartitionTable(blockDevice);

        assertTrue(table instanceof MasterBootRecord);
    }

    @Test
    public void testFileSystemCreate() throws Exception {
        BlockDeviceDriver blockDevice = new FileBlockDeviceDriver(
                new URL("https://www.dropbox.com/s/3bxngiqmwitlucd/mbr_fat32.img?dl=1"),
                2 * 512);
        blockDevice.init();

        PartitionTable table = PartitionTableFactory.createPartitionTable(blockDevice);

        assertTrue(table instanceof FileSystemPartitionTable);
    }

}