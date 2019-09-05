package com.github.mjdev.libaums.partition.fs

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.driver.ByteBlockDevice
import com.github.mjdev.libaums.fs.FileSystemFactory
import com.github.mjdev.libaums.partition.PartitionTable
import com.github.mjdev.libaums.partition.PartitionTableEntry
import com.github.mjdev.libaums.partition.PartitionTableFactory

import java.io.IOException

/**
 * Created by magnusja on 30/07/17.
 */

class FileSystemPartitionTableCreator : PartitionTableFactory.PartitionTableCreator {
    @Throws(IOException::class)
    override fun read(blockDevice: BlockDeviceDriver): PartitionTable? {
        return try {
            FileSystemPartitionTable(blockDevice,
                    FileSystemFactory.createFileSystem(PartitionTableEntry(0, 0, 0), ByteBlockDevice(blockDevice)))
        } catch (e: FileSystemFactory.UnsupportedFileSystemException) {
            null
        }

    }
}
