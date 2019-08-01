/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
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

import android.util.Log

/**
 * This class represents the command status wrapper (CSW) in the SCSI
 * transparent command set standard, which is transmitted from the device to the
 * host after the data phase (if any).
 *
 * @author mjahnen
 */
class CommandStatusWrapper {

    private var dCswSignature: Int = 0
    private var dCswTag: Int = 0
    private var dCswDataResidue: Int = 0
    private var bCswStatus: Byte = 0

    /**
     * Reads command block wrapper from the specified buffer and stores it into this object.
     *
     * @param buffer
     * The data where the command block wrapper is located.
     */
    fun read(buffer: ByteBuffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        dCswSignature = buffer.int
        if (dCswSignature != D_CSW_SIGNATURE) {
            Log.e(TAG, "unexpected dCSWSignature $dCswSignature")
        }
        dCswTag = buffer.int
        dCswDataResidue = buffer.int
        bCswStatus = buffer.get()
    }

    /**
     * Returns the tag which can be used to determine the corresponding
     * [ CBW][com.github.mjdev.libaums.driver.scsi.commands.CommandBlockWrapper].
     *
     * @return The command status wrapper tag.
     * @see com.github.mjdev.libaums.driver.scsi.commands.CommandBlockWrapper
     * .getdCswTag
     */
    fun getdCswTag(): Int {
        return dCswTag
    }

    /**
     * Returns the amount of bytes which has not been processed yet in the data
     * phase.
     *
     * @return The amount of bytes.
     */
    fun getdCswDataResidue(): Int {
        return dCswDataResidue
    }

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
    fun getbCswStatus(): Byte {
        return bCswStatus
    }

    companion object {

        /**
         * SCSI command has successfully been executed.
         */
        val COMMAND_PASSED = 0
        /**
         * SCSI command could not be executed, host should issue an SCSI request
         * sense.
         *
         * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiRequestSense
         */
        val COMMAND_FAILED = 1
        /**
         * SCSI command could not be executed, host should issue a mass storage
         * reset.
         */
        val PHASE_ERROR = 2

        /**
         * Every CSW has the same size.
         */
        val SIZE = 13

        private val TAG = CommandStatusWrapper::class.java.simpleName

        private val D_CSW_SIGNATURE = 0x53425355
    }
}
