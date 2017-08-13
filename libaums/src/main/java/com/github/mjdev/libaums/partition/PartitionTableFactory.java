/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.github.mjdev.libaums.partition;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.partition.fs.FileSystemPartitionTable;
import com.github.mjdev.libaums.partition.fs.FileSystemPartitionTableCreator;
import com.github.mjdev.libaums.partition.mbr.MasterBootRecord;
import com.github.mjdev.libaums.partition.mbr.MasterBootRecordCreator;

/**
 * Helper class to create different supported {@link PartitionTable}s.
 * 
 * @author mjahnen
 * 
 */
public class PartitionTableFactory {

    public static class UnsupportedPartitionTableException extends IOException {

    }

	public interface PartitionTableCreator {
        @Nullable  PartitionTable read(BlockDeviceDriver blockDevice) throws IOException;
    }

    private static List<PartitionTableCreator> partitionTables = new ArrayList<>();

    static {
        PartitionTableFactory.registerPartitionTable(new FileSystemPartitionTableCreator());
        PartitionTableFactory.registerPartitionTable(new MasterBootRecordCreator());
    }

	/**
	 * Creates a {@link PartitionTable} suitable for the given block device. The
	 * partition table should be located at the logical block address zero of
	 * the device.
	 * 
	 * @param blockDevice
	 *            The block device where the partition table is located.
	 * @return The newly created {@link PartitionTable}.
	 * @throws IOException
	 *             If reading from the device fails.
	 */
	public static PartitionTable createPartitionTable(BlockDeviceDriver blockDevice)
			throws IOException {
        for(PartitionTableCreator creator : partitionTables) {
            PartitionTable table = creator.read(blockDevice);
            if(table != null) {
                return table;
            }
        }

        throw new UnsupportedPartitionTableException();
	}

    public static synchronized void registerPartitionTable(PartitionTableCreator creator) {
        partitionTables.add(creator);
    }
}
