package com.github.mjdev.libaums.partition.gpt

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.partition.PartitionTable
import com.github.mjdev.libaums.partition.PartitionTableEntry
import java.io.IOException
import java.nio.ByteBuffer
import java.util.ArrayList

class GPT private constructor(): PartitionTable {

    // See also https://zh.wikipedia.org/wiki/GUID%E7%A3%81%E7%A2%9F%E5%88%86%E5%89%B2%E8%A1%

    private val partitions = ArrayList<PartitionTableEntry>()

    override val size: Int get() =  partitions.size * 128
    override val partitionTableEntries: List<PartitionTableEntry>
        get() = partitions

    companion object {

        const val EFI_PART = "EFI_PART"

        @Throws(IOException::class)
        fun read(blockDevice: BlockDeviceDriver): GPT? {
            val result = GPT()
            val buffer = ByteBuffer.allocate(Math.max(128, blockDevice.blockSize))
            blockDevice.read(512, buffer) // LBA 0

            if (String(buffer.array(), 0x00, 8) == EFI_PART) {
                blockDevice.read(1024L + (result.partitions.size * 128), buffer)
                while (buffer[0].toInt() != 0) {
                    val offset = 1024 + (result.partitions.size * 128)
                    val entry = PartitionTableEntry(0,
                        buffer.getInt(offset + 32), buffer.getInt(offset + 40))

                    result.partitions.add(entry)
                    blockDevice.read(offset + 128L, buffer)
                }
            } else {
                throw IOException("not a valid GPT")
            }

            return result
        }
    }

}