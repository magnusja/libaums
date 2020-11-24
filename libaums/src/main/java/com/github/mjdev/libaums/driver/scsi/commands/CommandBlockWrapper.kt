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
 * This class represents the command block wrapper (CBW) which is always wrapped
 * around a specific SCSI command in the SCSI transparent command set standard.
 *
 *
 * Every SCSI command shall extend this class, call the constructor
 * [.CommandBlockWrapper] with the desired
 * information. When transmitting the command, the
 * [.serialize] method has to be called!
 *
 * @author mjahnen, Derpalus
 */
abstract class CommandBlockWrapper
/**
 * Constructs a new command block wrapper with the given information which
 * can than easily be serialized with [.serialize].
 *
 * @param transferLength
 * The bytes which should be transferred in the following data
 * phase (Zero if no data phase).
 * @param direction
 * The direction the data shall be transferred in the data phase.
 * If there is no data phase it should be
 * [#NONE][com.github.mjdev.libaums.driver.scsi.commands.CommandBlockWrapper.Direction]
 * @param lun
 * The logical unit number the command is directed to.
 * @param cbwcbLength
 * The length in bytes of the scsi command.
 * @param bCbwDynamicSize
 * If the data length can change during read due to dynamic message length
 */
protected constructor(var dCbwDataTransferLength: Int,
                      /**
                       * Returns the direction in the data phase.
                       *
                       * @return The direction.
                       * @see com.github.mjdev.libaums.driver.scsi.commands.CommandBlockWrapper.Direction
                       * Direction
                       */
                      val direction: Direction,
                      private val bCbwLun: Byte,
                      /**
                       * The amount of bytes which should be transmitted in the data
                       * phase.
                       */
                      val bCbwcbLength: Byte,
                      val bCbwDynamicSize: Boolean = false) {


    /**
     * The tag which can be used to determine the corresponding
     * [ CBW][com.github.mjdev.libaums.driver.scsi.commands.CommandStatusWrapper].
     *
     * @see com.github.mjdev.libaums.driver.scsi.commands.CommandStatusWrapper
     */
    var dCbwTag: Int = 0
    var bmCbwFlags: Byte = 0

    /**
     * The direction of the data phase of the SCSI command.
     *
     * @author mjahnen
     */
    enum class Direction {
        /**
         * Means from device to host (Android).
         */
        IN,
        /**
         * Means from host (Android) to device.
         */
        OUT,
        /**
         * There is no data phase
         */
        NONE
    }

    init {
        if (direction == Direction.IN)
            bmCbwFlags = 0x80.toByte()
    }

    /**
     * Serializes the command block wrapper for transmission.
     *
     *
     * This method should be called in every subclass right before the specific
     * SCSI command serializes itself to the buffer!
     *
     * @param buffer
     * The buffer were the serialized data should be copied to.
     */
    open fun serialize(buffer: ByteBuffer) {
        buffer.apply {
            order(ByteOrder.LITTLE_ENDIAN)
            putInt(D_CBW_SIGNATURE)
            putInt(dCbwTag)
            putInt(dCbwDataTransferLength)
            put(bmCbwFlags)
            put(bCbwLun)
            put(bCbwcbLength)
        }
    }

    /**
     * Returns the data transfer length for (dynamic) messages
     *
     *
     * This method should be overridden in subclasses whose data length
     * changes as data is received (dynamic length messages).
     * The function returns the new total data transfer length.
     *
     * @param buffer
     * The buffer containing the received data so far.
     *
     * @return New total data transfer length
     */
    open fun dynamicSizeFromPartialResponse(buffer: ByteBuffer) : Int { throw NotImplementedError("If dynamic length possible override in subclass") }

    companion object {

        private const val D_CBW_SIGNATURE = 0x43425355
    }

}
