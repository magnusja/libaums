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

package com.github.mjdev.libaums.driver.scsi

import java.io.IOException
import java.nio.ByteBuffer
import java.util.Arrays

import android.util.Log

import com.github.mjdev.libaums.usb.UsbCommunication
import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.driver.scsi.commands.CommandBlockWrapper
import com.github.mjdev.libaums.driver.scsi.commands.CommandBlockWrapper.Direction
import com.github.mjdev.libaums.driver.scsi.commands.CommandStatusWrapper
import com.github.mjdev.libaums.driver.scsi.commands.ScsiInquiry
import com.github.mjdev.libaums.driver.scsi.commands.ScsiInquiryResponse
import com.github.mjdev.libaums.driver.scsi.commands.ScsiRead10
import com.github.mjdev.libaums.driver.scsi.commands.ScsiReadCapacity
import com.github.mjdev.libaums.driver.scsi.commands.ScsiReadCapacityResponse
import com.github.mjdev.libaums.driver.scsi.commands.ScsiTestUnitReady
import com.github.mjdev.libaums.driver.scsi.commands.ScsiWrite10

/**
 * This class is responsible for handling mass storage devices which follow the
 * SCSI standard. This class communicates with the mass storage device via the
 * different SCSI commands.
 *
 * @author mjahnen
 * @see com.github.mjdev.libaums.driver.scsi.commands
 */
class ScsiBlockDevice(private val usbCommunication: UsbCommunication) : BlockDeviceDriver {
    private val outBuffer: ByteBuffer
    private val cswBuffer: ByteBuffer

    override var blockSize: Int = 0
        private set
    private var lastBlockAddress: Int = 0

    private val writeCommand = ScsiWrite10()
    private val readCommand = ScsiRead10()
    private val csw = CommandStatusWrapper()

    init {
        outBuffer = ByteBuffer.allocate(31)
        cswBuffer = ByteBuffer.allocate(CommandStatusWrapper.SIZE)
    }

    /**
     * Issues a SCSI Inquiry to determine the connected device. After that it is
     * checked if the unit is ready. Logs a warning if the unit is not ready.
     * Finally the capacity of the mass storage device is read.
     *
     * @throws IOException
     * If initialing fails due to an unsupported device or if
     * reading fails.
     * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiInquiry
     *
     * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiInquiryResponse
     *
     * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiTestUnitReady
     *
     * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiReadCapacity
     *
     * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiReadCapacityResponse
     */
    @Throws(IOException::class)
    override fun init() {
        val inBuffer = ByteBuffer.allocate(36)
        val inquiry = ScsiInquiry(inBuffer.array().size.toByte())
        transferCommand(inquiry, inBuffer)
        inBuffer.clear()
        // TODO support multiple luns!
        val inquiryResponse = ScsiInquiryResponse.read(inBuffer)
        Log.d(TAG, "inquiry response: $inquiryResponse")

        if (inquiryResponse.peripheralQualifier.toInt() != 0 || inquiryResponse.peripheralDeviceType.toInt() != 0) {
            throw IOException("unsupported PeripheralQualifier or PeripheralDeviceType")
        }

        val testUnit = ScsiTestUnitReady()
        if (!transferCommand(testUnit, ByteBuffer.allocate(0))) {
            Log.w(TAG, "unit not ready!")
        }

        val readCapacity = ScsiReadCapacity()
        inBuffer.clear()
        transferCommand(readCapacity, inBuffer)
        inBuffer.clear()
        val readCapacityResponse = ScsiReadCapacityResponse.read(inBuffer)
        blockSize = readCapacityResponse.blockLength
        lastBlockAddress = readCapacityResponse.logicalBlockAddress

        Log.i(TAG, "Block size: $blockSize")
        Log.i(TAG, "Last block address: $lastBlockAddress")
    }

    /**
     * Transfers the desired command to the device. If the command has a data
     * phase the parameter `inBuffer` is used to store or read data
     * to resp. from it. The direction of the data phase is determined by
     * [#getDirection()][com.github.mjdev.libaums.driver.scsi.commands.CommandBlockWrapper]
     * .
     *
     *
     * Return value is true if the status of the command status wrapper is
     * successful (
     * [#getbCswStatus()][com.github.mjdev.libaums.driver.scsi.commands.CommandStatusWrapper]
     * ).
     *
     * @param command
     * The command which should be transferred.
     * @param inBuffer
     * The buffer used for reading or writing.
     * @return True if the transaction was successful.
     * @throws IOException
     * If something fails.
     */
    @Throws(IOException::class)
    private fun transferCommand(command: CommandBlockWrapper, inBuffer: ByteBuffer): Boolean {
        val outArray = outBuffer.array()
        Arrays.fill(outArray, 0.toByte())

        outBuffer.clear()
        command.serialize(outBuffer)
        outBuffer.clear()

        var written = usbCommunication.bulkOutTransfer(outBuffer)
        if (written != outArray.size) {
            throw IOException("Writing all bytes on command $command failed!")
        }

        val transferLength = command.getdCbwDataTransferLength()
        var read = 0
        if (transferLength > 0) {

            if (command.direction == Direction.IN) {
                do {
                    read += usbCommunication.bulkInTransfer(inBuffer)
                } while (read < transferLength)

                if (read != transferLength) {
                    throw IOException("Unexpected command size (" + read + ") on response to "
                            + command)
                }
            } else {
                written = 0
                do {
                    written += usbCommunication.bulkOutTransfer(inBuffer)
                } while (written < transferLength)

                if (written != transferLength) {
                    throw IOException("Could not write all bytes: $command")
                }
            }
        }


        // expecting csw now
        cswBuffer.clear()
        read = usbCommunication.bulkInTransfer(cswBuffer)
        if (read != CommandStatusWrapper.SIZE) {
            throw IOException("Unexpected command size while expecting csw")
        }
        cswBuffer.clear()

        csw.read(cswBuffer)
        if (csw.getbCswStatus().toInt() != CommandStatusWrapper.COMMAND_PASSED) {
            throw IOException("Unsuccessful Csw status: " + csw.getbCswStatus())
        }

        if (csw.getdCswTag() != command.getdCbwTag()) {
            throw IOException("wrong csw tag!")
        }

        return csw.getbCswStatus().toInt() == CommandStatusWrapper.COMMAND_PASSED
    }

    /**
     * This method reads from the device at the specific device offset. The
     * devOffset specifies at which block the reading should begin. That means
     * the devOffset is not in bytes!
     */
    @Synchronized
    @Throws(IOException::class)
    override fun read(devOffset: Long, dest: ByteBuffer) {
        //long time = System.currentTimeMillis();
        if (dest.remaining() % blockSize != 0) {
            throw IllegalArgumentException("dest.remaining() must be multiple of blockSize!")
        }

        readCommand.init(devOffset.toInt(), dest.remaining(), blockSize)
        //Log.d(TAG, "reading: " + read);

        transferCommand(readCommand, dest)
        dest.position(dest.limit())

        //Log.d(TAG, "read time: " + (System.currentTimeMillis() - time));
    }

    /**
     * This method writes from the device at the specific device offset. The
     * devOffset specifies at which block the writing should begin. That means
     * the devOffset is not in bytes!
     */
    @Synchronized
    @Throws(IOException::class)
    override fun write(devOffset: Long, src: ByteBuffer) {
        //long time = System.currentTimeMillis();
        if (src.remaining() % blockSize != 0) {
            throw IllegalArgumentException("src.remaining() must be multiple of blockSize!")
        }

        writeCommand.init(devOffset.toInt(), src.remaining(), blockSize)
        //Log.d(TAG, "writing: " + write);

        transferCommand(writeCommand, src)
        src.position(src.limit())

        //Log.d(TAG, "write time: " + (System.currentTimeMillis() - time));
    }

    companion object {

        private val TAG = ScsiBlockDevice::class.java.simpleName
    }
}
