package me.jahnen.libaums.fs.fat32

import me.jahnen.libaums.driver.BlockDeviceDriver
import me.jahnen.libaums.fs.FileSystem
import me.jahnen.libaums.fs.FileSystemCreator
import me.jahnen.libaums.partition.PartitionTableEntry
import java.io.IOException

/**
 * Created by magnusja on 28/02/17.
 */

class Fat32FileSystemCreator : FileSystemCreator {

    @Throws(IOException::class)
    override fun read(entry: PartitionTableEntry, blockDevice: BlockDeviceDriver): FileSystem? {
        return Fat32FileSystem.read(blockDevice)
    }
}
