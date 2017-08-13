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

package com.github.mjdev.libaums.fs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.fat32.Fat32FileSystemCreator;
import com.github.mjdev.libaums.partition.PartitionTableEntry;

/**
 * This is a helper class to create different supported file systems. The file
 * system is determined by {link
 * {@link com.github.mjdev.libaums.partition.PartitionTableEntry}.
 * 
 * @author mjahnen
 * 
 */
public class FileSystemFactory {

    public static class UnsupportedFileSystemException extends IOException {

    }

    private static List<FileSystemCreator> fileSystems = new ArrayList<>();
    private static TimeZone timeZone = TimeZone.getDefault();

    static {
        FileSystemFactory.registerFileSystem(new Fat32FileSystemCreator());
    }

	public static FileSystem createFileSystem(PartitionTableEntry entry,
			BlockDeviceDriver blockDevice) throws IOException, UnsupportedFileSystemException {
		for(FileSystemCreator creator : fileSystems) {
            FileSystem fs = creator.read(entry, blockDevice);
            if(fs != null) {
                return fs;
            }
        }

        throw new UnsupportedFileSystemException();
	}

    /**
     * Register a new file system.
     * @param creator The creator which is able to check if a {@link BlockDeviceDriver} is holding
     *                the correct type of file system and is able to instantiate a {@link FileSystem}
     *                instance.
     */
    public static synchronized void registerFileSystem(FileSystemCreator creator) {
        fileSystems.add(creator);
    }

    /**
     * Set the timezone a file system should use to decode timestamps, if the file system only stores
     * local date and time and has no reference which zone these timestamp correspond to. (True for
     * FAT32, e.g.)
     * @param zone The timezone to use.
     */
    public static void setTimeZone(TimeZone zone) {
        timeZone = zone;
    }

    public static TimeZone getTimeZone() {
        return timeZone;
    }
}
