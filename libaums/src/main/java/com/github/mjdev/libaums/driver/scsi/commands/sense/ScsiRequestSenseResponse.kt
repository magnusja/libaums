package com.github.mjdev.libaums.driver.scsi.commands.sense

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and

/**
 * Represents the response of a sense request.
 *
 *
 * The response data is received in the data phase
 *
 * @author Derpalus
 * @see com.github.mjdev.libaums.driver.scsi.commands.ScsiRequestSense
 */
class ScsiRequestSenseResponse private constructor() {


    /**
     * Returns if the information field is valid.
     *
     * @return information is valid.
     */
    var informationValid: Boolean = false
        private set

    /**
     * Returns the error code.
     *
     * Apparently always either 0x70 or 0x71. Don't know the significance of this number.
     *
     * @return the error code.
     */
    var errorCode: Byte = 0
        private set

    /**
     * Returns the segment number.
     *
     * @return the segment number.
     */
    var segmentNumber: Byte = 0
        private set

    /**
     * Returns overall error/info type for this sense request.
     *
     * 0h = No Sense — no specific sense key information to be reported. A sense key of 0 indicates a successful command.
     * 1h = Recovered error — Indicates that the command completed successfully, with some recovery action performed by the device server.
     * 2h = Not Ready — Indicates that the logical unit is not accessible. Operator intervention may be required to correct this condition.
     * 3h = Medium Error — the command terminated with a non-recovered error condition that may have been caused by a flaw in the medium or an error in the recorded data.
     * 4h = Hardware Error — the device detected an unrecoverable hardware failure while performing the command or during a self-test.
     * 5h = Illegal Request — an illegal parameter in the command descriptor block or in the parameter list data.
     * 6h = Unit Attention — a power-on or reset has occurred to the device, or a not ready-to-ready transition has occurred, or an I/O element has been accessed. Also, this may indicate mode parameters have changed, or the microcode has been changed.
     * 7h = Data Protect — Indicates that a command that reads or writes the medium was attempted on a block that is protected. The read or write operation is not performed.
     * 8h = Blank Check — Indicates that a write-once device or a sequential-access device encountered blank medium or format-defined end-of-data indication while reading or that a write-once device encountered a non-blank medium while writing.
     * 9h = Vendor Specific
     * Ah = Copy Aborted — Indicates an EXTENDED COPY command was aborted due to an error condition on the source device, the destination device, or both.
     * Bh = Aborted Command — the device aborted the command. The initiator might be able to recover by trying the command again.
     * Ch = Reserved
     * Dh = Volume Overflow — Indicates that a buffered SCSI device has reached the end-of-partition and data may remain in the buffer that has not been written to the medium.
     * Eh = Miscompare — Indicates that the source data did not match the data read from the medium.
     * Fh = Completed — Indicates there is command completed sense data (see SAM-5) to be reported. This may occur for a successful command.
     *
     * @return the sense key.
     */
    var senseKey: Byte = 0
        private set

    /**
     * Returns the information field.
     *
     * @return the information field.
     */
    var information: Int = 0
        private set

    /**
     * Returns the length of the rest of the message.
     *
     * A request sense message must always be at least 18 bytes, but can be longer.
     * additionalSenseLength indicates the total message length minus 8. I.e. if
     * it returns 10 the message is just the 18 initial bytes, if it's 12 there are
     * two more bytes to collect. Consider additionalSenseLength to be the length
     * of the remainder of the message after the byte that holds additionalSenseLength.
     *
     * @return the additional sense length.
     */
    var additionalSenseLength: Byte = 0
        private set

    /**
     * Returns the specific command information.
     *
     * @return the specific command information.
     */
    var commandSpecificInformation: Int = 0
        private set

    /**
     * Returns the additional sense code (ASC).
     *
     * This code together with ASCQ contains more specific information
     * in addition to senseKey.
     *
     * @return the additional sense code.
     *
     * @see
     * <a href="https://docs.oracle.com/en/storage/tape-storage/storagetek-sl150-modular-tape-library/slorm/request-sense-03h.html">https://docs.oracle.com/en/storage/tape-storage/storagetek-sl150-modular-tape-library/slorm/request-sense-03h.html</a>
     * <a href="https://www.seagate.com/files/staticfiles/support/docs/manual/Interface manuals/100293068k.pdf">https://www.seagate.com/files/staticfiles/support/docs/manual/Interface manuals/100293068k.pdf (page 59 - 64)</a>
     */
    var additionalSenseCode: Byte = 0
        private set

    /**
     * Returns the additional sense code qualifier (ASCQ).
     *
     * This code together with ASC contains more specific information
     * in addition to senseKey.
     *
     * @return the additional sense code qualifier.
     *
     * @see
     * <a href="https://docs.oracle.com/en/storage/tape-storage/storagetek-sl150-modular-tape-library/slorm/request-sense-03h.html">https://docs.oracle.com/en/storage/tape-storage/storagetek-sl150-modular-tape-library/slorm/request-sense-03h.html</a>
     * <a href="https://www.seagate.com/files/staticfiles/support/docs/manual/Interface manuals/100293068k.pdf">https://www.seagate.com/files/staticfiles/support/docs/manual/Interface manuals/100293068k.pdf (page 59 - 64)</a>
     */
    var additionalSenseCodeQualifier: Byte = 0
        private set

    /**
     * Checks the result from a request sense command.
     * Will throw some type of SenseException, or do nothing
     * if sense response indicates {@see ScsiRequestSenseResponse.NO_SENSE},
     * {@see ScsiRequestSenseResponse.COMPLETED},
     * {@see ScsiRequestSenseResponse.RECOVERED_ERROR}.
     *
     * @throws SenseException (multiple variants)
     */
    fun checkResponseForError() {
        when (senseKey.toInt()) {
            NO_SENSE, COMPLETED, RECOVERED_ERROR -> { } // success, nothing to do
            NOT_READY -> handleNotReady()
            MEDIUM_ERROR -> handleMediumError()
            HARDWARE_ERROR -> throw HardwareError(this)
            ILLEGAL_REQUEST -> throw IllegalCommand(this)
            UNIT_ATTENTION -> throw UnitAttention(this)
            DATA_PROTECT -> throw DataProtect(this)
            BLANK_CHECK -> throw BlankCheck(this)
            COPY_ABORTED -> throw CopyAborted(this)
            ABORTED -> throw Aborted(this)
            VOLUME_OVERFLOW -> throw VolumeOverflow(this)
            MISCOMPARE -> throw Miscompare(this)
        }

        throw SenseException(this, "Sense exception: " + this.senseKey)
    }

    private fun handleNotReady() {
        if (additionalSenseCode.toInt() == 0x04) {
            // Logical unit issues
            when (additionalSenseCodeQualifier.toInt()) {
                0x01 -> throw NotReadyTryAgain(this)
                0x03 -> throw ManualIntervention(this)
                0x04 -> throw NotReadyTryAgain(this)
                0x07 -> throw NotReadyTryAgain(this)
                0x09 -> throw NotReadyTryAgain(this)
                0x22 -> throw RestartRequired(this)
                0x12 -> throw NotReady(this, "Not ready; logical unit offline")
            }
        } else if (additionalSenseCode.toInt() == 0x3A) {
            throw MediaNotInserted(this)
        }
        throw NotReady(this)
    }

    private fun handleMediumError() {
        when (additionalSenseCode.toInt()) {
            0x0C -> throw MediumError(this, "Write error")
            0x11 -> throw MediumError(this, "Read error")
            0x31 -> throw MediumError(this, "Storage medium corrupted")
        }
        throw MediumError(this)
    }

    companion object {

        /**
         * Constructs a new object with the given data.
         *
         * @param buffer
         * The data where the [.ScsiRequestSenseResponse] is
         * located.
         * @return The parsed [.ScsiRequestSenseResponse].
         */
        fun read(buffer: ByteBuffer): ScsiRequestSenseResponse {
            buffer.order(ByteOrder.BIG_ENDIAN)

            return ScsiRequestSenseResponse().apply {
                errorCode = buffer.get()
                segmentNumber = buffer.get()
                senseKey = buffer.get()
                information = buffer.int
                additionalSenseLength = buffer.get()
                commandSpecificInformation = buffer.int
                additionalSenseCode = buffer.get()
                additionalSenseCodeQualifier = buffer.get();


                informationValid = (errorCode.toInt() and 0x80) > 0
                errorCode = errorCode and 0x7F
                senseKey = senseKey and 0x0F
            }
        }

        /**
         * @see senseKey
         */
        const val NO_SENSE = 0
        const val RECOVERED_ERROR = 1
        const val NOT_READY = 2
        const val MEDIUM_ERROR = 3
        const val HARDWARE_ERROR = 4
        const val ILLEGAL_REQUEST = 5
        const val UNIT_ATTENTION = 6
        const val DATA_PROTECT = 7
        const val BLANK_CHECK = 8
        const val COPY_ABORTED = 10
        const val ABORTED = 11
        const val VOLUME_OVERFLOW = 13
        const val MISCOMPARE = 14
        const val COMPLETED = 15
    }
}