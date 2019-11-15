/*
 * (C) Copyright 2014 mjahnen <github@mgns.tech>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.github.mjdev.libaums.driver.scsi.commands

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * SCSI command to write to the mass storage device. The 10 means that the
 * transfer length is two byte and the logical block address field is four byte.
 * Thus the hole command takes 10 byte when serialized.
 *
 *
 * The actual data is transferred in the data phase.
 *
 * @author mjahnen
 */
class ScsiWrite10 : CommandBlockWrapper {

    private var blockAddress: Int = 0
    private var transferBytes: Int = 0
    private var blockSize: Int = 0
    private var transferBlocks: Short = 0

    /**
     * Constructs a new write command without any information.
     * Be sure to call [.init] before transfering command to device.
     */
    constructor(lun: Byte) : super(0, Direction.OUT, lun, LENGTH)

    /**
     * Constructs a new write command with the given information.
     *
     * @param blockAddress
     * The logical block address the write should start.
     * @param transferBytes
     * The bytes which should be transferred.
     * @param blockSize
     * The block size of the mass storage device.
     */
    constructor(blockAddress: Int, transferBytes: Int, blockSize: Int) : super(transferBytes, Direction.OUT, 0.toByte(), LENGTH) {
        init(blockAddress, transferBytes, blockSize)
    }

    fun init(blockAddress: Int, transferBytes: Int, blockSize: Int) {
        super.dCbwDataTransferLength = transferBytes
        this.blockAddress = blockAddress
        this.transferBytes = transferBytes
        this.blockSize = blockSize
        val transferBlocks = (transferBytes / blockSize).toShort()
        require(transferBytes % blockSize == 0) { "transfer bytes is not a multiple of block size" }
        this.transferBlocks = transferBlocks
    }

    override fun serialize(buffer: ByteBuffer) {
        super.serialize(buffer)

        buffer.apply {
            order(ByteOrder.BIG_ENDIAN)
            put(OPCODE)
            put(0.toByte())
            putInt(blockAddress)
            put(0.toByte())
            putShort(transferBlocks)
        }
    }

    override fun toString(): String {
        return ("ScsiWrite10 [blockAddress=" + blockAddress + ", transferBytes=" + transferBytes
                + ", blockSize=" + blockSize + ", transferBlocks=" + transferBlocks
                + ", getdCbwDataTransferLength()=" + dCbwDataTransferLength + "]")
    }

    companion object {

        // private static final String TAG = ScsiWrite10.class.getSimpleName();
        private const val LENGTH: Byte = 10
        private const val OPCODE: Byte = 0x2a
    }

}
