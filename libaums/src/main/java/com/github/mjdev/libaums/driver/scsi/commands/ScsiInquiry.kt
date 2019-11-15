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

/**
 * This class represents a SCSI Inquiry command. It is used to get important
 * information about the connected mass storage device. This information include
 * the supported SCSI commands.
 *
 *
 * The response is sent in the data phase.
 *
 * @author mjahnen
 * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiInquiryResponse
 */
class ScsiInquiry(private val allocationLength: Byte, lun: Byte) : CommandBlockWrapper(allocationLength.toInt(), Direction.IN, lun, LENGTH) {

    override fun serialize(buffer: ByteBuffer) {
        super.serialize(buffer)
        buffer.apply {
            put(OPCODE)
            put(0.toByte())
            put(0.toByte())
            put(0.toByte())
            put(allocationLength)
        }
    }

    companion object {
        private const val LENGTH: Byte = 0x6
        private const val OPCODE: Byte = 0x12
    }

}
