package com.github.magnusja.libaums.javafs.wrapper.device;

import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;

import org.jnode.driver.block.FSBlockDeviceAPI;
import org.jnode.partitions.PartitionTable;
import org.jnode.partitions.PartitionTableEntry;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by magnusja on 2/28/17.
 */

public class FSBlockDeviceWrapper implements FSBlockDeviceAPI {

    private static final String TAG = FSBlockDeviceWrapper.class.getSimpleName();
    private BlockDeviceDriver blockDevice;
    private com.github.mjdev.libaums.partition.PartitionTableEntry partitionTableEntry;

    public FSBlockDeviceWrapper(BlockDeviceDriver blockDevice, com.github.mjdev.libaums.partition.PartitionTableEntry partitionTableEntry) {
        this.blockDevice = blockDevice;
        this.partitionTableEntry = partitionTableEntry;
    }

    @Override
    public int getSectorSize() throws IOException {
        return blockDevice.getBlockSize();
    }

    @Override
    public PartitionTableEntry getPartitionTableEntry() {
        return new PartitionTableEntry() {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public boolean hasChildPartitionTable() {
                return false;
            }

            @Override
            public PartitionTable<?> getChildPartitionTable() {
                return null;
            }
        };
    }

    @Override
    public long getLength() throws IOException {
        return partitionTableEntry.getTotalNumberOfSectors() * blockDevice.getBlockSize();
    }

    @Override
    public void read(long l, ByteBuffer byteBuffer) throws IOException {
        blockDevice.read(l, byteBuffer);
    }

    @Override
    public void write(long l, ByteBuffer byteBuffer) throws IOException {
        blockDevice.write(l, byteBuffer);
    }

    @Override
    public void flush() throws IOException {

    }
}
