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
 * Represents the command to read the capacity from the mass storage device.
 *
 *
 * The data is transferred in the data phase.
 *
 * @author mjahnen
 * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiReadCapacityResponse
 */
class ScsiReadCapacity(lun: Byte) : CommandBlockWrapper(RESPONSE_LENGTH, Direction.IN, lun, LENGTH) {

    override fun serialize(buffer: ByteBuffer) {
        super.serialize(buffer)
        buffer.put(OPCODE)
    }

    companion object {

        private const val RESPONSE_LENGTH = 0x8
        private const val LENGTH: Byte = 10
        private const val OPCODE: Byte = 0x25
    }

}
