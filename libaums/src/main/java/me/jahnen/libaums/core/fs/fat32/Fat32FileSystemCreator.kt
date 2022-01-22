package me.jahnen.libaums.core.fs.fat32

import me.jahnen.libaums.core.driver.BlockDeviceDriver
import me.jahnen.libaums.core.fs.FileSystem
import me.jahnen.libaums.core.fs.FileSystemCreator
import me.jahnen.libaums.core.partition.PartitionTableEntry
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
