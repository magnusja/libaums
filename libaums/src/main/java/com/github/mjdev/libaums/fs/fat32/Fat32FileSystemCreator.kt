package com.github.mjdev.libaums.fs.fat32

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.fs.FileSystem
import com.github.mjdev.libaums.fs.FileSystemCreator
import com.github.mjdev.libaums.fs.fat32.Fat32FileSystem
import com.github.mjdev.libaums.partition.PartitionTableEntry
import com.github.mjdev.libaums.partition.PartitionTypes

import java.io.IOException
import java.util.ArrayList

/**
 * Created by magnusja on 28/02/17.
 */

class Fat32FileSystemCreator : FileSystemCreator {

    @Throws(IOException::class)
    override fun read(entry: PartitionTableEntry, blockDevice: BlockDeviceDriver): FileSystem? {
        return Fat32FileSystem.read(blockDevice)
    }
}
