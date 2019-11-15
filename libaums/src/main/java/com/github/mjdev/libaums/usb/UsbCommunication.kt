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

package com.github.mjdev.libaums.usb

import java.io.IOException
import java.nio.ByteBuffer

/**
 * This Interface describes a low level device to perform USB transfers. At the
 * moment only bulk IN and OUT transfer are supported. Every class that follows
 * [com.github.mjdev.libaums.driver.BlockDeviceDriver] can use this to
 * communicate with the underlying USB stack.
 *
 * @author mjahnen
 */
interface UsbCommunication {

    /**
     * Performs a bulk out transfer beginning at the offset specified in the
     * `buffer` of length `buffer#remaining()`.
     *
     * @param src
     * The data to transfer.
     * @return Bytes transmitted if successful.
     */
    @Throws(IOException::class)
    fun bulkOutTransfer(src: ByteBuffer): Int

    /**
     * Performs a bulk in transfer beginning at offset zero in the
     * `buffer` of length `buffer#remaining()`.
     *
     * @param dest
     * The buffer where data should be transferred.
     * @return Bytes read if successful.
     */
    @Throws(IOException::class)
    fun bulkInTransfer(dest: ByteBuffer): Int

    companion object {
        const val TRANSFER_TIMEOUT = 5000
    }
}
