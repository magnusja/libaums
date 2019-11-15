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
 * This command is used to determine if the logical unit of the mass storage
 * device is ready. Sometimes this command fails even if the unit can process
 * all commands successfully. Thus this command issues only a warning in the
 * [com.github.mjdev.libaums.driver.scsi.ScsiBlockDevice].
 *
 *
 * This command has no data phase, the result is determined by
 * [#getbCswStatus()][com.github.mjdev.libaums.driver.scsi.commands.CommandStatusWrapper].
 *
 * @author mjahnen
 */
class ScsiTestUnitReady(lun: Byte) : CommandBlockWrapper(0, Direction.NONE, lun, LENGTH) {

    override fun serialize(buffer: ByteBuffer) {
        super.serialize(buffer)
        buffer.put(OPCODE)
    }

    companion object {

        private const val LENGTH: Byte = 0x6
        private const val OPCODE: Byte = 0x0
    }

}
