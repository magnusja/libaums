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

package com.github.mjdev.libaums.fs.fat;

import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.BootSector;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.fat32.FatDirectory;
import com.github.mjdev.libaums.partition.FatType;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * This class represents the FAT32 file system and is responsible for setting
 * the FAT32 file system up and extracting the volume label and the root
 * directory.
 *
 * @author mjahnen
 */
public class Fat16FileSystem implements FileSystem {

    private final String TAG = Fat16FileSystem.class.getSimpleName();
    private BootSector bootSector;
    private Fat fat;
    private FatDirectory rootDirectory;

    /**
     * This method constructs a FAT12/16 file system for the given block device.
     * There are no further checks that the block device actually represents a
     * valid FAT12/16 file system. That means it must be ensured that the device
     * actually holds a FAT12/16 file system in advance!
     *
     * @param blockDevice The block device the FAT12/16 file system is located.
     * @throws IOException If reading from the device fails.
     */
    private Fat16FileSystem(BlockDeviceDriver blockDevice, FatType fatType) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        blockDevice.read(0, buffer);
        bootSector = Fat16BootSector.read(buffer,fatType);
        fat = new Fat(blockDevice, bootSector,fatType);
        rootDirectory = FatDirectory.readRoot(blockDevice, fat, bootSector);
    }

    /**
     * This method constructs a FAT12/16 file system for the given block device.
     * There are no further checks if the block device actually represents a
     * valid FAT32 file system. That means it must be ensured that the device
     * actually holds a FAT32 file system in advance!
     *
     * @param blockDevice The block device the FAT12/16 file system is located.
     * @throws IOException If reading from the device fails.
     */
    public static Fat16FileSystem read(BlockDeviceDriver blockDevice, FatType fatType) throws IOException {
        return new Fat16FileSystem(blockDevice, fatType);
    }

    @Override
    public UsbFile getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public String getVolumeLabel() {
        String volumeLabel = rootDirectory.getVolumeLabel();
        Log.d(TAG, "Volume name by root:"+volumeLabel);
        if (volumeLabel == null) {
            volumeLabel = bootSector.getVolumeLabel();
            Log.d(TAG, "Volume name by boot sector:"+volumeLabel);
        }
        return volumeLabel;
    }

    @Override
    public long getCapacity() {
        return bootSector.getTotalNumberOfSectors() * bootSector.getBytesPerSector();
    }

    @Override
    public long getOccupiedSpace() {
        return getCapacity() - getFreeSpace();
    }

    @Override
    public long getFreeSpace() {
        return /*fat.getFreeClusterCount() * bootSector.getBytesPerCluster()*/-1;
    }
}
