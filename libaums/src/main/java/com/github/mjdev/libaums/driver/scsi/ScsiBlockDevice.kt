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

package com.github.mjdev.libaums.driver.scsi

import android.util.Log
import com.github.mjdev.libaums.driver.BlockDeviceDriver
import com.github.mjdev.libaums.driver.scsi.commands.*
import com.github.mjdev.libaums.driver.scsi.commands.CommandBlockWrapper.Direction
import com.github.mjdev.libaums.driver.scsi.commands.sense.*
import com.github.mjdev.libaums.usb.PipeException
import com.github.mjdev.libaums.usb.UsbCommunication
import java.io.IOException
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is responsible for handling mass storage devices which follow the
 * SCSI standard. This class communicates with the mass storage device via the
 * different SCSI commands.
 *
 * @author mjahnen, Derpalus
 * @see com.github.mjdev.libaums.driver.scsi.commands
 */
class ScsiBlockDevice(private val usbCommunication: UsbCommunication, private val lun: Byte) : BlockDeviceDriver {
    private val outBuffer: ByteBuffer = ByteBuffer.allocate(31)
    private val cswBuffer: ByteBuffer = ByteBuffer.allocate(CommandStatusWrapper.SIZE)

    override var blockSize: Int = 0
        private set
    private var lastBlockAddress: Int = 0

    private val writeCommand = ScsiWrite10(lun=lun)
    private val readCommand = ScsiRead10(lun=lun)
    private val csw = CommandStatusWrapper()

    private var cbwTagCounter = 1

    /**
     * The size of the block device, in blocks of [blockSize] bytes,
     *
     * @return The block device size in blocks
     */
    override val blocks: Long = lastBlockAddress.toLong()

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
        for(i in 0..MAX_RECOVERY_ATTEMPTS) {
            try {
                initAttempt()
                return
            } catch(e: InitRequired) {
                Log.i(TAG, e.message ?: "Reinitializing device")
            } catch (e: NotReadyTryAgain) {
                Log.i(TAG, e.message ?: "Reinitializing device")
            }
            Thread.sleep(100)
        }

        throw IOException("MAX_RECOVERY_ATTEMPTS Exceeded while trying to init communication with USB device, please reattach device and try again")
    }

    @Throws(IOException::class)
    private fun initAttempt() {
        val inBuffer = ByteBuffer.allocate(36)
        val inquiry = ScsiInquiry(inBuffer.array().size.toByte(), lun=lun)
        transferCommand(inquiry, inBuffer)
        inBuffer.clear()
        val inquiryResponse = ScsiInquiryResponse.read(inBuffer)
        Log.d(TAG, "inquiry response: $inquiryResponse")

        if (inquiryResponse.peripheralQualifier.toInt() != 0 || inquiryResponse.peripheralDeviceType.toInt() != 0) {
            throw IOException("unsupported PeripheralQualifier or PeripheralDeviceType")
        }

        val testUnit = ScsiTestUnitReady(lun=lun)
        transferCommandWithoutDataPhase(testUnit)

        val readCapacity = ScsiReadCapacity(lun=lun)
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
     * @throws IOException
     * If something fails.
     */
    @Throws(IOException::class)
    private fun transferCommand(command: CommandBlockWrapper, inBuffer: ByteBuffer) {
        for(i in 0..MAX_RECOVERY_ATTEMPTS) {
            try {
                val result = transferOneCommand(command, inBuffer)
                val senseWasNotIssued = handleCommandResult(result)
                if (senseWasNotIssued || command.direction == Direction.NONE) {
                    // successful w/o need of sending sense command
                    // OR
                    // command has no data phase ie. no need to sent again
                    // and read response into buffer
                    return
                }

                // sense command was sent because of error, sense was successful
                // ie. NO_SENSE, RECOVERED_ERROR, COMPLETED, see
                // sense response impl
                // try again and hope that data phase ie. filling inBuffer works now

            } catch (e: SenseException) {
                Log.w(TAG, (e.message ?: "SenseException"))
                when(e) {
                    is InitRequired -> init()
                    is NotReadyTryAgain -> {} // try again
                    else -> throw e
                }
            } catch(e: PipeException) {
                Log.w(TAG, (e.message ?: "PipeException") + ", try bulk storage reset and retry")
                bulkOnlyMassStorageReset()
            } catch (e: IOException) {
                // Retry
                Log.w(TAG, (e.message ?: "IOException") + ", retrying...")
            }

            Thread.sleep(100)
        }

        throw IOException("MAX_RECOVERY_ATTEMPTS Exceeded while trying to transfer command to device, please reattach device and try again")
    }

    @Throws(IOException::class)
    private fun transferCommandWithoutDataPhase(command: CommandBlockWrapper) {
        require(command.direction == Direction.NONE) { "Command has a data phase" }
        transferCommand(command, ByteBuffer.allocate(0))
    }

    @Throws(IOException::class)
    private fun handleCommandResult(status: Int): Boolean {
        return when (status) {
            CommandStatusWrapper.COMMAND_PASSED -> true
            CommandStatusWrapper.COMMAND_FAILED -> {
                requestSense()
                false
            }
            CommandStatusWrapper.PHASE_ERROR -> {
                bulkOnlyMassStorageReset()
                throw IOException("phase error, please reattach device and try again")
            }
            else -> throw IllegalStateException("CommandStatus wrapper illegal status $status")
        }
    }

    @Throws(IOException::class)
    private fun requestSense() {
        val inBuffer = ByteBuffer.allocate(18)
        val sense = ScsiRequestSense(inBuffer.array().size.toByte(), lun = lun)
        when (val status = transferOneCommand(sense, inBuffer)) {
            CommandStatusWrapper.COMMAND_PASSED -> {
                inBuffer.clear()
                val response = ScsiRequestSenseResponse.read(inBuffer)
                response.checkResponseForError()
            }
            CommandStatusWrapper.COMMAND_FAILED -> throw IOException("requesting sense failed")
            CommandStatusWrapper.PHASE_ERROR -> {
                bulkOnlyMassStorageReset()
                throw IOException("phase error, please reattach device and try again")
            }
            else -> throw IllegalStateException("CommandStatus wrapper illegal status $status")
        }
    }

    @Throws(IOException::class)
    private fun bulkOnlyMassStorageReset() {
        Log.w(TAG, "sending bulk only mass storage request")
        val bArr = ByteArray(2)
        // REQUEST_BULK_ONLY_MASS_STORAGE_RESET = 255
        // REQUEST_TYPE_BULK_ONLY_MASS_STORAGE_RESET = 33
        val transferred: Int = usbCommunication.controlTransfer(33, 255, 0, usbCommunication.usbInterface.id, bArr, 0)
        if (transferred == -1) {
            throw IOException("bulk only mass storage reset failed!")
        }
        Log.d(TAG, "Trying to clear halt on both endpoints")
        usbCommunication.clearFeatureHalt(usbCommunication.inEndpoint)
        usbCommunication.clearFeatureHalt(usbCommunication.outEndpoint)
    }

    @Throws(IOException::class)
    private fun transferOneCommand(command: CommandBlockWrapper, inBuffer: ByteBuffer): Int {
        val outArray = outBuffer.array()
        Arrays.fill(outArray, 0.toByte())

        command.dCbwTag = cbwTagCounter
        cbwTagCounter++

        outBuffer.clear()
        command.serialize(outBuffer)
        outBuffer.clear()

        var written = usbCommunication.bulkOutTransfer(outBuffer)
        if (written != outArray.size) {
            throw IOException("Writing all bytes on command $command failed!")
        }

        var transferLength = command.dCbwDataTransferLength
        inBuffer.clear()
        inBuffer.limit(transferLength)

        var read = 0
        if (transferLength > 0) {

            if (command.direction == Direction.IN) {
                do {
                    read += usbCommunication.bulkInTransfer(inBuffer)
                    if (command.bCbwDynamicSize) {
                        transferLength = command.dynamicSizeFromPartialResponse(inBuffer)
                        inBuffer.limit(transferLength)
                    }
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
        if (csw.dCswTag != command.dCbwTag) {
            throw IOException("wrong csw tag!")
        }

        return csw.bCswStatus.toInt()
    }

    /**
     * This method reads from the device at the specific device offset. The
     * devOffset specifies at which block the reading should begin. That means
     * the devOffset is not in bytes!
     */
    @Synchronized
    @Throws(IOException::class)
    override fun read(deviceOffset: Long, buffer: ByteBuffer) {
        //long time = System.currentTimeMillis();
        require(buffer.remaining() % blockSize == 0) { "buffer.remaining() must be multiple of blockSize!" }

        readCommand.init(deviceOffset.toInt(), buffer.remaining(), blockSize)
        //Log.d(TAG, "reading: " + read);

        transferCommand(readCommand, buffer)
        buffer.position(buffer.limit())

        //Log.d(TAG, "read time: " + (System.currentTimeMillis() - time));
    }

    /**
     * This method writes from the device at the specific device offset. The
     * devOffset specifies at which block the writing should begin. That means
     * the devOffset is not in bytes!
     */
    @Synchronized
    @Throws(IOException::class)
    override fun write(deviceOffset: Long, buffer: ByteBuffer) {
        //long time = System.currentTimeMillis();
        require(buffer.remaining() % blockSize == 0) { "buffer.remaining() must be multiple of blockSize!" }

        writeCommand.init(deviceOffset.toInt(), buffer.remaining(), blockSize)
        //Log.d(TAG, "writing: " + write);

        transferCommand(writeCommand, buffer)
        buffer.position(buffer.limit())

        //Log.d(TAG, "write time: " + (System.currentTimeMillis() - time));
    }

    companion object {
        private const val MAX_RECOVERY_ATTEMPTS = 20
        private val TAG = ScsiBlockDevice::class.java.simpleName
    }
}
