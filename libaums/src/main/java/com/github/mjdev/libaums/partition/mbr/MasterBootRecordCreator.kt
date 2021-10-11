package com.github.mjdev.libaums.partition.mbr

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.partition.PartitionTable
import com.github.mjdev.libaums.partition.PartitionTableFactory
import com.github.mjdev.libaums.partition.gpt.GPT
import com.github.mjdev.libaums.partition.gpt.GPTCreator
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Created by magnusja on 30/07/17.
 */

class MasterBootRecordCreator : PartitionTableFactory.PartitionTableCreator {
    @Throws(IOException::class)
    override fun read(blockDevice: BlockDeviceDriver): PartitionTable? {
        val buffer = ByteBuffer.allocate(Math.max(512, blockDevice.blockSize))
        blockDevice.read(512, buffer)
        return if (String(buffer.array(), 0x00, 8) == GPT.EFI_PART) {
            GPTCreator().read(blockDevice)
        } else {
            blockDevice.read(0, buffer)
            MasterBootRecord.read(buffer)
        }
    }
}
