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
 * Represents the response of a read capacity request.
 *
 *
 * The response data is received in the data phase
 *
 * @author mjahnen
 * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiReadCapacity
 */
class ScsiReadCapacityResponse private constructor() {

    /**
     * Returns the address of the last accessible block on the block device.
     *
     *
     * The size of the device is then last accessible block + 0!
     *
     * @return The last block address.
     */
    var logicalBlockAddress: Int = 0
        private set
    /**
     * Returns the size of each block in the block device.
     *
     * @return The block size in bytes.
     */
    var blockLength: Int = 0
        private set

    companion object {

        /**
         * Constructs a new object with the given data.
         *
         * @param buffer
         * The data where the [.ScsiReadCapacityResponse] is
         * located.
         * @return The parsed [.ScsiReadCapacityResponse].
         */
        fun read(buffer: ByteBuffer): ScsiReadCapacityResponse {
            buffer.order(ByteOrder.BIG_ENDIAN)

            return ScsiReadCapacityResponse().apply {
                logicalBlockAddress = buffer.int
                blockLength = buffer.int
            }
        }
    }
}