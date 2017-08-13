package com.github.mjdev.libaums.partition.mbr;

import android.support.annotation.Nullable;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.partition.PartitionTable;
import com.github.mjdev.libaums.partition.PartitionTableFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by magnusja on 30/07/17.
 */

public class MasterBootRecordCreator implements PartitionTableFactory.PartitionTableCreator {
    @Nullable
    @Override
    public PartitionTable read(BlockDeviceDriver blockDevice) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        blockDevice.read(0, buffer);
        return MasterBootRecord.read(buffer);
    }
}
