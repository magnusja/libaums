package com.github.mjdev.libaums.partition.gpt

import android.util.Log
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import me.jahnen.libaums.core.partition.PartitionTable
import me.jahnen.libaums.core.partition.PartitionTableEntry
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList

class GPT private constructor(): PartitionTable {

    // See also https://en.wikipedia.org/wiki/GUID_Partition_Table

    private val partitions = ArrayList<PartitionTableEntry>()

    override val size: Int get() =  partitions.size * 128
    override val partitionTableEntries: List<PartitionTableEntry>
        get() = partitions

    companion object {
        private val TAG = GPT::class.java.simpleName
        const val EFI_PART = "EFI PART"

        const val GPT_OFFSET = 512  // GPT has a protective MBR, GPT starts after

        const val ENTRY_SIZE = 128

        const val FIRST_LBA_OFFSET = 32
        const val LAST_LBA_OFFSET = 40

        @Throws(IOException::class)
        fun read(blockDevice: BlockDeviceDriver): GPT? {
            val result = GPT()
            var buffer = ByteBuffer.allocate(512 * 2)
            blockDevice.read(0, buffer)

            val efiTestString = String(buffer.array(), GPT_OFFSET, 8, Charsets.US_ASCII)
            Log.d(TAG, "EFI test string $efiTestString")

            if (efiTestString != EFI_PART) {
                return null
            }
            Log.d(TAG, "EFI test string matches!")


            buffer = ByteBuffer.allocate(512 * 34) // at LBA 34 GPT should stop
            blockDevice.read(0, buffer)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            var entry_offset = 1024

            while (buffer[entry_offset].toInt() != 0) {
                val entry = PartitionTableEntry(-1, // Unknown
                    buffer.getLong(entry_offset + FIRST_LBA_OFFSET).toInt(),
                    buffer.getLong(entry_offset + LAST_LBA_OFFSET).toInt())

                result.partitions.add(entry)

                entry_offset += ENTRY_SIZE
            }


            return result
        }
    }

}