package com.github.mjdev.libaums.driver.scsi.commands.sense

import java.io.IOException

open class SenseException(response: ScsiRequestSenseResponse?, msg: String) :
        IOException(msg + responseToString(response)) {

    /**
     * @see ScsiRequestSenseResponse
     */
    val senseKey: Byte = response?.senseKey ?: 0
    val additionalSenseCode = response?.additionalSenseCode ?: 0
    val additionalSenseCodeQualifier = response?.additionalSenseCodeQualifier ?: 0

    companion object {
        private fun responseToString(response: ScsiRequestSenseResponse?): String {
            return if (response == null) {
                "";
            } else {
                " (ASC: " + response.additionalSenseCode +
                        ", ASCQ: " + response.additionalSenseCodeQualifier + ")"
            }
        }
    }
}


/**
 * Must re-initialize the device as it was reset
 *
 * Recommended action: either stop and tell user to unplug and re-plug the device,
 * or stop and automatically re-initialize the device.
 */
class InitRequired(response: ScsiRequestSenseResponse?):
        SenseException(response, "Device must be re-initialized")

/**
 * User must power cycle device.
 *
 * Recommended action: stop and tell user to unplug and re-plug the device.
 */
class RestartRequired(response: ScsiRequestSenseResponse?):
        SenseException(response, "Device must be power cycled")

/**
 * Can't recover from this error. Device is impossible to
 * communicate with at this time.
 *
 * Recommended action: stop and tell user that it's impossible
 * to communicate with the device.
 */
class Unrecoverable(response: ScsiRequestSenseResponse?):
        SenseException(response, "Can't communicate with device")


/**
 * Device is not ready.
 *
 * Recommended action: stop and tell user that the device is not ready.
 */
open class NotReady(response: ScsiRequestSenseResponse?, msg: String = "Not ready"):
        SenseException(response, msg)

/**
 * Device is not ready yet but will be eventually, try again later.
 *
 * Recommended action: retry command again a bit later.
 */
class NotReadyTryAgain(response: ScsiRequestSenseResponse?):
        NotReady(response, "Not ready; try again later")

/**
 * The error can't be solved without manual intervention.
 *
 * Recommended action: stop and tell user that manual intervention is required.
 */
class ManualIntervention(response: ScsiRequestSenseResponse?, msg: String = "Manual intervention required"):
        SenseException(response, msg)

/**
 * The current LUN does not have a storage media inserted (open SD card slot).
 *
 * Recommended action: ignore for this LUN and possibly tell the user.
 */
class MediaNotInserted(response: ScsiRequestSenseResponse?):
        SenseException(response, "Storage media not inserted")

/**
 * An error in the storage medium.
 *
 * Recommended action: stop and inform user.
 */
class MediumError(response: ScsiRequestSenseResponse?, msg: String = "Error in the storage medium"):
        SenseException(response, msg)

/**
 * A hardware error.
 *
 * Recommended action: stop and inform user.
 */
class HardwareError(response: ScsiRequestSenseResponse?):
        SenseException(response, "Hardware error")

/**
 * An illegal command. This is something that should be fixed by the programmer.
 *
 * Recommended action: fix.
 */
class IllegalCommand(response: ScsiRequestSenseResponse?):
        SenseException(response, "Illegal command")

/**
 * Something has changed in the device such as a power on, reset,
 * not ready-to-ready transition, disk space full, mode change, too hot or too cold.
 * Insertions and removals of SD cards probably show up here as well.
 *
 * Recommended action: stop and inform user. Possibly recoverable for certain
 * combinations of ASC + ASCQs.
 */
class UnitAttention(response: ScsiRequestSenseResponse?):
        SenseException(response, "Unit attention")

/**
 * Data medium is read or write protected.
 *
 * Recommended action: stop and tell user to disable read/write protection.
 */
class DataProtect(response: ScsiRequestSenseResponse?):
        SenseException(response, "Medium is read/write protected")

/**
 * Data medium is probably a write once medium that has already been written.
 *
 * Recommended action: stop and tell user.
 */
class BlankCheck(response: ScsiRequestSenseResponse?):
        SenseException(response, "Blank medium check failed")

/**
 * Copy aborted.
 *
 * Recommended action: stop and tell user.
 */
class CopyAborted(response: ScsiRequestSenseResponse?):
        SenseException(response, "Copy aborted")

/**
 * Command aborted. Could be due to Logical Unit communication failure or time-out.
 * Can also have several other reasons such as parity errors and CRC check fails.
 * Internal data transfer errors.
 *
 * Recommended action: stop and tell user.
 */
class Aborted(response: ScsiRequestSenseResponse?):
        SenseException(response, "Command aborted")

/**
 * Volume overflow. Probably due to the device running out of disk space?
 *
 * Recommended action: stop and tell user.
 */
class VolumeOverflow(response: ScsiRequestSenseResponse?):
        SenseException(response, "Volume overflow")

/**
 * Data miscompare. Don't know what this means, possibly a
 * failed read/write validity check?
 *
 * Recommended action: stop and tell user.
 */
class Miscompare(response: ScsiRequestSenseResponse?):
        SenseException(response, "Miscompare")