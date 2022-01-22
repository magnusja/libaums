package me.jahnen.libaums.fs

import me.jahnen.libaums.driver.BlockDeviceDriver
import me.jahnen.libaums.partition.PartitionTableEntry

import java.io.IOException

/**
 * Created by magnusja on 28/02/17.
 */

interface FileSystemCreator {
    @Throws(IOException::class)
    fun read(entry: PartitionTableEntry, blockDevice: BlockDeviceDriver): FileSystem?
}
