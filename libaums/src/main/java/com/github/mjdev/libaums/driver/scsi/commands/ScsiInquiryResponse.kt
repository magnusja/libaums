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
import kotlin.experimental.and

/**
 * This class represents the response of a SCSI Inquiry. It holds various
 * information about the mass storage device.
 *
 *
 * This response is received in the data phase.
 *
 * @author mjahnen
 * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiInquiry
 */
class ScsiInquiryResponse private constructor() {

    /**
     *
     * @return Zero if a device is connected to the unit.
     */
    var peripheralQualifier: Byte = 0
        private set
    /**
     * The type of the mass storage device.
     *
     * @return Zero for a direct access block device.
     */
    var peripheralDeviceType: Byte = 0
        private set
    /**
     *
     * @return True if the media can be removed (eg. card reader).
     */
    var isRemovableMedia: Boolean = false
        internal set
    /**
     * This method returns the version of the SCSI Primary Commands (SPC)
     * standard the device supports.
     *
     * @return Version of the SPC standard
     */
    var spcVersion: Byte = 0
        internal set
    var responseDataFormat: Byte = 0
        internal set

    override fun toString(): String {
        return ("ScsiInquiryResponse [peripheralQualifier=" + peripheralQualifier
                + ", peripheralDeviceType=" + peripheralDeviceType + ", removableMedia="
                + isRemovableMedia + ", spcVersion=" + spcVersion + ", responseDataFormat="
                + responseDataFormat + "]")
    }

    companion object {

        /**
         * Constructs a new object with the given data.
         *
         * @param buffer
         * The data where the [.ScsiInquiryResponse] is located.
         * @return The parsed [.ScsiInquiryResponse].
         */
        fun read(buffer: ByteBuffer): ScsiInquiryResponse {
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            val b = buffer.get()

            return ScsiInquiryResponse().apply {
                peripheralQualifier = b and 0xe0.toByte()
                peripheralDeviceType = b and 0x1f.toByte()
                isRemovableMedia = buffer.get().toInt() == 0x80
                spcVersion = buffer.get()
                responseDataFormat = buffer.get() and 0x7.toByte()
            }
        }
    }
}
