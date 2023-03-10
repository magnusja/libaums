package com.github.mjdev.libaums.partition.gpt

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.partition.PartitionTable
import com.github.mjdev.libaums.partition.PartitionTableFactory
import java.nio.ByteBuffer

class GPTCreator: PartitionTableFactory.PartitionTableCreator {
    override fun read(blockDevice: BlockDeviceDriver): PartitionTable? = GPT.read(blockDevice)
}