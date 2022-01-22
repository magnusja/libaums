package me.jahnen.libaums.partition.fs

import me.jahnen.libaums.driver.BlockDeviceDriver
import me.jahnen.libaums.driver.ByteBlockDevice
import me.jahnen.libaums.fs.FileSystemFactory
import me.jahnen.libaums.partition.PartitionTable
import me.jahnen.libaums.partition.PartitionTableEntry
import me.jahnen.libaums.partition.PartitionTableFactory

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
