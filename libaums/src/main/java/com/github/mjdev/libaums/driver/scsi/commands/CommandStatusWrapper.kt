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

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This class represents the command status wrapper (CSW) in the SCSI
 * transparent command set standard, which is transmitted from the device to the
 * host after the data phase (if any).
 *
 * @author mjahnen
 */
class CommandStatusWrapper {

    private var dCswSignature: Int = D_CSW_SIGNATURE

    /**
     * Returns the tag which can be used to determine the corresponding
     * [ CBW][com.github.mjdev.libaums.driver.scsi.commands.CommandBlockWrapper].
     *
     * @return The command status wrapper tag.
     * @see com.github.mjdev.libaums.driver.scsi.commands.CommandBlockWrapper
     * .getdCswTag
     */
    var dCswTag: Int = 0
        private set

    /**
     * Returns the amount of bytes which has not been processed yet in the data
     * phase.
     *
     * @return The amount of bytes.
     */
    var dCswDataResidue: Int = 0
        private set
    /**
     * Returns the status of execution of the transmitted SCSI command.
     *
     * @return The status.
     * @see com.github.mjdev.libaums.driver.scsi.commands.CommandStatusWrapper
     * .COMMAND_PASSED
     *
     * @see com.github.mjdev.libaums.driver.scsi.commands.CommandStatusWrapper
     * .COMMAND_FAILED
     *
     * @see com.github.mjdev.libaums.driver.scsi.commands.CommandStatusWrapper
     * .PHASE_ERROR
     */
    var bCswStatus: Byte = 0
        private set

     /**
     * Reads command block wrapper from the specified buffer and stores it into this object.
     *
     * @param buffer
     * The data where the command block wrapper is located.
     */
    fun read(buffer: ByteBuffer) {
         buffer.order(ByteOrder.LITTLE_ENDIAN)

         this.apply {

            dCswSignature = buffer.int
            if (dCswSignature != D_CSW_SIGNATURE) {
                Log.e(TAG, "unexpected dCSWSignature $dCswSignature")
            }
            dCswTag = buffer.int
            dCswDataResidue = buffer.int
            bCswStatus = buffer.get()
         }
    }

    companion object {

        /**
         * SCSI command has successfully been executed.
         */
        const val COMMAND_PASSED = 0
        /**
         * SCSI command could not be executed, host should issue an SCSI request
         * sense.
         *
         * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiRequestSense
         */
        const val COMMAND_FAILED = 1
        /**
         * SCSI command could not be executed, host should issue a mass storage
         * reset.
         */
        const val PHASE_ERROR = 2

        /**
         * Every CSW has the same size.
         */
        const val SIZE = 13

        private val TAG = CommandStatusWrapper::class.java.simpleName

        private const val D_CSW_SIGNATURE = 0x53425355
    }
}
