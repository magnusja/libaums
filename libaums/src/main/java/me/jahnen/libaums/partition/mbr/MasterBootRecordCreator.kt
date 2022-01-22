package me.jahnen.libaums.partition.mbr

import me.jahnen.libaums.driver.BlockDeviceDriver
import me.jahnen.libaums.partition.PartitionTable
import me.jahnen.libaums.partition.PartitionTableFactory
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Created by magnusja on 30/07/17.
 */

class MasterBootRecordCreator : PartitionTableFactory.PartitionTableCreator {
    @Throws(IOException::class)
    override fun read(blockDevice: BlockDeviceDriver): PartitionTable? {
        val buffer = ByteBuffer.allocate(Math.max(512, blockDevice.blockSize))
        blockDevice.read(0, buffer)
        return MasterBootRecord.read(buffer)
    }
}
