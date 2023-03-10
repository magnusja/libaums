package me.jahnen.libaums.core.fs;

import me.jahnen.libaums.core.driver.BlockDeviceDriver;
import me.jahnen.libaums.core.driver.ByteBlockDevice;
import me.jahnen.libaums.core.driver.file.FileBlockDeviceDriver;
import me.jahnen.libaums.core.fs.fat32.Fat32FileSystem;
import me.jahnen.libaums.core.fs.fat32.Fat32FileSystemCreator;
import me.jahnen.libaums.core.partition.PartitionTableEntry;
import me.jahnen.libaums.core.partition.PartitionTypes;

import org.junit.Test;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by magnusja on 12/08/17.
 */
public class FileSystemFactoryTest {
    @Test
    public void createFat32FileSystem() throws Exception {
        BlockDeviceDriver blockDevice = createDevice();
        PartitionTableEntry entry = createPartitionTable();
        FileSystem fs = FileSystemFactory.INSTANCE.createFileSystem(entry, blockDevice);

        assertTrue(fs instanceof Fat32FileSystem);
    }

    @Test
    public void fileSystemPriority() throws Exception  {
        ArrayList<String> orderTracker = new ArrayList<>();

        // Clear and register with varying priorities to verify creators are invoked in expected order
        FileSystemFactory.clearFileSystems();
        FileSystemFactory.registerFileSystem(mockCreator(orderTracker,"not called"), FileSystemFactory.DEFAULT_PRIORITY + 4);
        FileSystemFactory.registerFileSystem(mockCreator(orderTracker,"third"), FileSystemFactory.DEFAULT_PRIORITY + 1);
        FileSystemFactory.registerFileSystem(mockCreator(orderTracker,"first"));
        FileSystemFactory.registerFileSystem(mockCreator(orderTracker,"fourth"), FileSystemFactory.DEFAULT_PRIORITY + 2);
        FileSystemFactory.registerFileSystem(mockCreator(orderTracker,"second"));
        FileSystemFactory.registerFileSystem(new Fat32FileSystemCreator(), FileSystemFactory.DEFAULT_PRIORITY + 3);
        FileSystemFactory.registerFileSystem(mockCreator(orderTracker,"not called"), FileSystemFactory.DEFAULT_PRIORITY + 5);

        BlockDeviceDriver blockDevice = createDevice();
        PartitionTableEntry entry = createPartitionTable();
        FileSystem fs = FileSystemFactory.INSTANCE.createFileSystem(entry, blockDevice);

        assertEquals(orderTracker, Arrays.asList("first", "second", "third", "fourth"));
        assertTrue(fs instanceof Fat32FileSystem);

        // Since this is a singleton try to return it to its original state for other tests
        FileSystemFactory.clearFileSystems();
        FileSystemFactory.registerFileSystem(new Fat32FileSystemCreator(), FileSystemFactory.DEFAULT_PRIORITY + 1);
    }

    private FileSystemCreator mockCreator(ArrayList<String> orderTracker, String name)  {
        return (entry, blockDevice) -> {
            orderTracker.add(name);
            return null;
        };
    }

    private BlockDeviceDriver createDevice() throws Exception {
        BlockDeviceDriver blockDevice = new ByteBlockDevice(new FileBlockDeviceDriver(
                new URL("https://www.dropbox.com/s/3bxngiqmwitlucd/mbr_fat32.img?dl=1"),
                2 * 512));
        blockDevice.init();

        return blockDevice;
    }

    private PartitionTableEntry createPartitionTable() {
        return new PartitionTableEntry(PartitionTypes.FAT32, 2 * 512, 1337);
    }
}