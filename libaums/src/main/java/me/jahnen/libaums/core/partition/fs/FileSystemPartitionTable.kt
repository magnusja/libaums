package me.jahnen.libaums.core.partition.fs

import android.util.Log
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import me.jahnen.libaums.core.fs.FileSystem
import me.jahnen.libaums.core.partition.PartitionTable
import me.jahnen.libaums.core.partition.PartitionTableEntry
import java.util.*

/**
 * Represents a dummy partition table. Sometimes devices do not have an MBR or GPT to save memory.
 * https://stackoverflow.com/questions/38004064/is-it-possible-that-small-sd-cards-are-formatted-without-an-mbr
 * Actual File System is then reevaluated in a later stage in [com.github.mjdev.libaums.fs.FileSystemFactory].
 */
class FileSystemPartitionTable(blockDevice: BlockDeviceDriver, fs: FileSystem) : PartitionTable {

    internal var entries: MutableList<PartitionTableEntry> = ArrayList()

    override val size: Int
        get() = 0

    override val partitionTableEntries: List<PartitionTableEntry>
        get() = entries

    init {
        Log.i(TAG, "Found a device without partition table, yay!")
        val totalNumberOfSectors = fs.capacity / blockDevice.blockSize
        if (fs.capacity % blockDevice.blockSize != 0L) {
            Log.w(TAG, "fs capacity is not multiple of block size")
        }
        entries.add(PartitionTableEntry(fs.type, 0, totalNumberOfSectors))
    }

    companion object {

        private val TAG = FileSystemPartitionTable::class.java.simpleName
    }
}
