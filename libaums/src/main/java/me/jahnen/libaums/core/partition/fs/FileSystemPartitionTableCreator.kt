package me.jahnen.libaums.core.partition.fs

import me.jahnen.libaums.core.driver.BlockDeviceDriver
import me.jahnen.libaums.core.driver.ByteBlockDevice
import me.jahnen.libaums.core.fs.FileSystemFactory
import me.jahnen.libaums.core.partition.PartitionTable
import me.jahnen.libaums.core.partition.PartitionTableEntry
import me.jahnen.libaums.core.partition.PartitionTableFactory

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
