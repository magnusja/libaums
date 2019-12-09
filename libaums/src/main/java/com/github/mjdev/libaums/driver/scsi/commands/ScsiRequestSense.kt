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
 * This class is used to issue a SCSI request sense when a command has failed.
 *
 * @author mjahnen
 * @see com.github.mjdev.libaums.driver.scsi.commands.CommandStatusWrapper
 * .getbCswStatus
 */
class ScsiRequestSense(private val allocationLength: Byte, lun: Byte) : CommandBlockWrapper(0, Direction.NONE, lun, LENGTH) {

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
        private const val OPCODE: Byte = 0x3
        private const val LENGTH: Byte = 0x6
    }

}
