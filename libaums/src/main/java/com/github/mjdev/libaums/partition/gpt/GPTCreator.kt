package com.github.mjdev.libaums.partition.gpt

import me.jahnen.libaums.core.driver.BlockDeviceDriver
import me.jahnen.libaums.core.partition.PartitionTable
import me.jahnen.libaums.core.partition.PartitionTableFactory


class GPTCreator: PartitionTableFactory.PartitionTableCreator {
    override fun read(blockDevice: BlockDeviceDriver): PartitionTable? = GPT.read(blockDevice)
}