package com.github.mjdev.libaums.driver.scsi

import com.github.mjdev.libaums.driver.scsi.commands.ScsiRequestSenseResponse
import java.nio.ByteBuffer

class SenseParser {

    companion object{
        /**
         * Checks the result from a request sense command.
         * Will always throw some type of SenseException, although
         * some variants are easily recoverable.
         *
         *
         * @param buffer
         * The buffer containing the request sense result.
         * @throws SenseException (multiple variants)
         * Always throws. Exception indicates if something is wrong or needs handling.
         */
        fun parse(buffer: ByteBuffer) {
            buffer.clear()
            val response = ScsiRequestSenseResponse.read(buffer)

            when (response.senseKey.toInt()) {
                ScsiRequestSenseResponse.NO_SENSE -> throw Recovered(response)
                ScsiRequestSenseResponse.COMPLETED -> throw Recovered(response)
                ScsiRequestSenseResponse.RECOVERED_ERROR -> throw Recovered(response)
                ScsiRequestSenseResponse.NOT_READY -> handleNotReady(response)
                ScsiRequestSenseResponse.MEDIUM_ERROR -> handleMediumError(response)
                ScsiRequestSenseResponse.HARDWARE_ERROR -> throw HardwareError(response)
                ScsiRequestSenseResponse.ILLEGAL_REQUEST -> throw IllegalCommand(response)
                ScsiRequestSenseResponse.UNIT_ATTENTION -> throw UnitAttention(response)
                ScsiRequestSenseResponse.DATA_PROTECT -> throw DataProtect(response)
                ScsiRequestSenseResponse.BLANK_CHECK -> throw BlankCheck(response)
                ScsiRequestSenseResponse.COPY_ABORTED -> throw CopyAborted(response)
                ScsiRequestSenseResponse.ABORTED -> throw Aborted(response)
                ScsiRequestSenseResponse.VOLUME_OVERFLOW -> throw VolumeOverflow(response)
                ScsiRequestSenseResponse.MISCOMPARE -> throw Miscompare(response)
            }

            throw SenseException(response, "Sense exception: " + response.senseKey)
        }

        private fun handleNotReady(response: ScsiRequestSenseResponse) {
            if (response.additionalSenseCode.toInt() == 0x04) {
                // Logical unit issues
                when (response.additionalSenseCodeQualifier.toInt()) {
                    0x01 -> throw NotReadyTryAgain(response)
                    0x03 -> throw ManualIntervention(response)
                    0x04 -> throw NotReadyTryAgain(response)
                    0x07 -> throw NotReadyTryAgain(response)
                    0x09 -> throw NotReadyTryAgain(response)
                    0x22 -> throw RestartRequired(response)
                    0x12 -> throw NotReady(response, "Not ready; logical unit offline")
                }
            } else if (response.additionalSenseCode.toInt() == 0x3A) {
                throw MediaNotInserted(response)
            }
            throw NotReady(response)
        }

        private fun handleMediumError(response: ScsiRequestSenseResponse) {
            when (response.additionalSenseCode.toInt()) {
                0x0C -> throw MediumError(response, "Write error")
                0x11 -> throw MediumError(response, "Read error")
                0x31 -> throw MediumError(response, "Storage medium corrupted")
            }
            throw MediumError(response)
        }
    }
}
