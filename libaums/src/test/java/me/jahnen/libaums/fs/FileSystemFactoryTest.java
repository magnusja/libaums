package me.jahnen.libaums.fs;

import me.jahnen.libaums.driver.BlockDeviceDriver;
import me.jahnen.libaums.driver.ByteBlockDevice;
import me.jahnen.libaums.driver.file.FileBlockDeviceDriver;
import me.jahnen.libaums.fs.fat32.Fat32FileSystem;
import me.jahnen.libaums.partition.PartitionTableEntry;
import me.jahnen.libaums.partition.PartitionTypes;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * Created by magnusja on 12/08/17.
 */
public class FileSystemFactoryTest {
    @Test
    public void createFat32FileSystem() throws Exception {
        BlockDeviceDriver blockDevice = new ByteBlockDevice(new FileBlockDeviceDriver(
                new URL("https://www.dropbox.com/s/3bxngiqmwitlucd/mbr_fat32.img?dl=1"),
                2 * 512));
        blockDevice.init();

        PartitionTableEntry entry = new PartitionTableEntry(PartitionTypes.FAT32, 2 * 512, 1337);
        FileSystem fs = FileSystemFactory.INSTANCE.createFileSystem(entry, blockDevice);

        assertTrue(fs instanceof Fat32FileSystem);
    }

}