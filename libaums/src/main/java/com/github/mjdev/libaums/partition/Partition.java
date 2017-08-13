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

import java.io.IOException;
import java.nio.ByteBuffer;

import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.driver.ByteBlockDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.FileSystemFactory;

/**
 * This class represents a partition on an mass storage device. A partition has
 * a certain file system which can be accessed via {@link #getFileSystem()}.
 * This file system is needed to to access the files and directories of a
 * partition.
 * <p>
 * The method {@link #getVolumeLabel()} returns the volume label for the
 * partition. Calling the method is equivalent to calling
 * {@link FileSystem#getVolumeLabel()}.
 * 
 * @author mjahnen
 * 
 */
public class Partition extends ByteBlockDevice {

	private static final String TAG = Partition.class.getSimpleName();

	/**
	 * The logical block address where on the device this partition starts.
	 */
	private FileSystem fileSystem;

	private Partition(BlockDeviceDriver blockDevice, PartitionTableEntry entry) {
		super(blockDevice, entry.getLogicalBlockAddress());
	}

	/**
	 * Creates a new partition with the information given.
	 * 
	 * @param entry
	 *            The entry the partition shall represent.
	 * @param blockDevice
	 *            The underlying block device. This block device must already been initialized, see
	 *            {@link BlockDeviceDriver#init()}.
	 * @return The newly created Partition.
	 * @throws IOException
	 *             If reading from the device fails.
	 */
	public static Partition createPartition(PartitionTableEntry entry, BlockDeviceDriver blockDevice)
			throws IOException {
		try {
			Partition partition = new Partition(blockDevice, entry);
			// TODO weird triangle relationship between partiton and fs??
			FileSystem fs = FileSystemFactory.createFileSystem(entry, partition);
			partition.fileSystem = fs;
			return partition;
		} catch (FileSystemFactory.UnsupportedFileSystemException e) {
			Log.w(TAG, "Unsupported fs on partition");
			return null;
		}
	}

	/**
	 * 
	 * @return the file system on the partition which can be used to access
	 *         files and directories.
	 */
	public FileSystem getFileSystem() {
		return fileSystem;
	}

	/**
	 * This method returns the volume label of the file system / partition.
	 * Calling this method is equivalent to calling
	 * {@link FileSystem#getVolumeLabel()}.
	 * 
	 * @return Returns the volume label of this partition.
	 */
	public String getVolumeLabel() {
		return fileSystem.getVolumeLabel();
	}
}
