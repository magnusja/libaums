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

package com.github.mjdev.libaums.fs.fat32

import com.github.mjdev.libaums.fs.FileSystemFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and

/**
 * This class holds the information of an 32 byte FAT32 directory entry. It
 * holds information such as if an entry is a directory, read only or hidden.
 *
 *
 * There are three cases a [.FatDirectoryEntry] is used:
 *
 *  * To store information about a directory or a file.
 *  * To store a part of a long filename. Every entry can store up to 13 bytes
 * of a long file name.
 *  * To store a volume label which can occur in the root directory of a FAT32
 * file system.
 *
 *
 *
 * To determine if the entry denotes a volume label entry use
 * [.isVolumeLabel]. If this method returns true only the method
 * [.getVolumeLabel] makes sense to call. Calling any other method the
 * results will be undefined.
 *
 *
 * To determine if an entry is an entry for a long file name use
 * [.isLfnEntry]. If this method returns true only the method
 * [.extractLfnPart] makes sense to call. Calling any other
 * method the results will be undefined.
 *
 *
 * In all other cases the entry is either a file or a directory which can be
 * determined by [.isDirectory]. Further information of the file or
 * directory give for example [.getCreatedDateTime] or
 * [.getStartCluster] to access the contents.
 *
 * @author mjahnen
 */
internal class FatDirectoryEntry
/**
 * Constructs a new [.FatDirectoryEntry] with the given data.
 *
 * @param data
 * The buffer where entry is located.
 */ private constructor(
        /**
         * Holds the data like it would be stored on the disk.
         */
        private var data: ByteBuffer = ByteBuffer.allocate(SIZE)) {

    /**
     * The 8.3 short name of the entry, when entry represents a directory or
     * file.
     */
    var shortName: ShortName? = null
        get() {
            return if (data.get(0).toInt() == 0)
                null
            else {
                field
            }
        }
        set(sn) {
            field = sn
            // clear just in case
            data.clear()
            field?.serialize(data)
            // clear buffer because short name put 13 bytes
            data.clear()
        }


    /**
     * Returns the flags in the [.FatDirectoryEntry].
     *
     * @return The flag variable.
     * @see .setFlag
     * @see .FLAG_ARCHIVE
     *
     * @see .FLAG_DIRECTORY
     *
     * @see .FLAG_HIDDEN
     *
     * @see .FLAG_SYSTEM
     *
     * @see .FLAG_READONLY
     *
     * @see .FLAG_VOLUME_ID
     */
    private val flags: Int
        get() = data.get(ATTR_OFF).toInt()

    val isShortNameLowerCase: Boolean
        get() = data.get(SHORTNAME_CASE_OFF) and 0x8.toByte() != 0.toByte()

    val isShortNameExtLowerCase: Boolean
        get() = data.get(SHORTNAME_CASE_OFF) and 0x10.toByte() != 0.toByte()

    /**
     * Returns true if the current [.FatDirectoryEntry] is an long
     * filename entry.
     *
     * @return True if the entry is a long filename entry.
     * @see .extractLfnPart
     * @see .createLfnPart
     */
    val isLfnEntry: Boolean
        get() = isHidden && isVolume && isReadOnly && isSystem

    /**
     * Returns true if the current [.FatDirectoryEntry] denotes a
     * directory.
     *
     * @return True if entry is a directory.
     */
    val isDirectory: Boolean
        get() = flags and (FLAG_DIRECTORY or FLAG_VOLUME_ID) == FLAG_DIRECTORY

    /**
     * Returns true if the current [.FatDirectoryEntry] denotes a volume
     * label.
     *
     * @return True if entry is a volume label.
     * @see .getVolumeLabel
     * @see .createVolumeLabel
     */
    val isVolumeLabel: Boolean
        get() = if (isLfnEntry)
            false
        else
            flags and (FLAG_DIRECTORY or FLAG_VOLUME_ID) == FLAG_VOLUME_ID

    /**
     * Returns true if the current [.FatDirectoryEntry] is a system file
     * or directory. Normally a user shall not see this item!
     *
     * @return True if entry is a system item.
     */
    val isSystem: Boolean
        get() = isFlagSet(FLAG_SYSTEM)

    /**
     * Returns true if the current [.FatDirectoryEntry] is hidden. This
     * entry should only be accessible by the user if he explicitly asks for it!
     *
     * @return True if entry is hidden.
     */
    val isHidden: Boolean
        get() = isFlagSet(FLAG_HIDDEN)

    /**
     * Returns true if the current [.FatDirectoryEntry] is an archive.
     * This is used by backup tools.
     *
     * @return True if entry is an archive.
     */
    val isArchive: Boolean
        get() = isFlagSet(FLAG_ARCHIVE)

    /**
     * Returns true if the current [.FatDirectoryEntry] is a read only
     * file or directory. Normally a user shall not be able to write or to alter
     * this item!
     *
     * @return True if entry is read only.
     */
    val isReadOnly: Boolean
        get() = isFlagSet(FLAG_READONLY)

    /**
     * Returns true if the volume id flag is set.
     *
     * @return True if volume id set.
     * @see .FLAG_VOLUME_ID
     */
    val isVolume: Boolean
        get() = isFlagSet(FLAG_VOLUME_ID)

    /**
     * Returns true if the [.FatDirectoryEntry] was deleted and shall
     * not show up in the file tree.
     *
     * @return True if entry was deleted.
     */
    val isDeleted: Boolean
        get() = getUnsignedInt8(0) == ENTRY_DELETED

    /**
     * Time in milliseconds since January 1 00:00:00, 1970 UTC
     */
    var createdDateTime: Long
        // TODO entry has also field which holds 10th seconds created
        get() = decodeDateTime(getUnsignedInt16(CREATED_DATE_OFF),
                getUnsignedInt16(CREATED_TIME_OFF))
        set(dateTime) {
            setUnsignedInt16(CREATED_DATE_OFF, encodeDate(dateTime))
            setUnsignedInt16(CREATED_TIME_OFF, encodeTime(dateTime))
        }

    /**
     * Time in milliseconds since January 1 00:00:00, 1970 UTC
     */
    var lastModifiedDateTime: Long
        get() = decodeDateTime(getUnsignedInt16(LAST_WRITE_DATE_OFF),
                getUnsignedInt16(LAST_WRITE_TIME_OFF))
        set(dateTime) {
            setUnsignedInt16(LAST_WRITE_DATE_OFF, encodeDate(dateTime))
            setUnsignedInt16(LAST_WRITE_TIME_OFF, encodeTime(dateTime))
        }

    /**
     * Time in milliseconds since January 1 00:00:00, 1970 UTC.
     */
    var lastAccessedDateTime: Long
        get() = decodeDateTime(getUnsignedInt16(LAST_ACCESSED_DATE_OFF), 0)
        set(dateTime) = setUnsignedInt16(LAST_ACCESSED_DATE_OFF, encodeDate(dateTime))

    /**
     * Returns the volume label which can occur in the root directory of a FAT32
     * file system.
     *
     * @return The volume label.
     * @see .createVolumeLabel
     * @see .isVolumeLabel
     */
    val volumeLabel: String
        get() {
            val builder = StringBuilder()

            for (i in 0..10) {
                val b = data.get(i)
                if (b.toInt() == 0)
                    break
                builder.append(b.toChar())
            }

            return builder.toString()
        }

    /**
     * Returns the start cluster for this [.FatDirectoryEntry]. The
     * start cluster denotes where a file or a directory start in the fs.
     */
    var startCluster: Long
        get() {
            val msb = getUnsignedInt16(MSB_CLUSTER_OFF)
            val lsb = getUnsignedInt16(LSB_CLUSTER_OFF)
            return (msb.toLong() shl 16) or lsb.toLong()
        }
        set(newStartCluster) {
            setUnsignedInt16(MSB_CLUSTER_OFF, (newStartCluster shr 16 and 0xffff).toInt())
            setUnsignedInt16(LSB_CLUSTER_OFF, (newStartCluster and 0xffff).toInt())
        }

    /**
     * The size of a file in bytes. For directories this value should
     * always be zero.
     */
    var fileSize: Long
        get() = getUnsignedInt32(FILE_SIZE_OFF)
        set(newSize) = setUnsignedInt32(FILE_SIZE_OFF, newSize)


    /**
     * Creates a completely new [.FatDirectoryEntry]. Do not forget to
     * set the start cluster! The time fields ([.setCreatedDateTime]
     * , [.setLastAccessedDateTime],
     * [.setLastModifiedDateTime]) are all set to the current time.
     *
     * @return The newly constructed entry.
     * @see .setStartCluster
     * @see .setDirectory
     */
    constructor() : this(data = ByteBuffer.allocate(SIZE)) {
        val now = System.currentTimeMillis()
        createdDateTime = now
        lastAccessedDateTime = now
        lastModifiedDateTime = now
    }

    init {
        data.order(ByteOrder.LITTLE_ENDIAN)
        shortName = ShortName.parse(data)
        data.clear()
    }

    /**
     * Serializes this [.FatDirectoryEntry] so that it can be written to
     * disk. Updates the position of the given buffer.
     *
     * @param buffer
     * The buffer data shall be written to.
     */
    fun serialize(buffer: ByteBuffer) {
        buffer.put(data.array())
    }

    /**
     * Sets a specific flag.
     *
     * @param flag
     * The flag to set.
     * @see .getFlags
     * @see .FLAG_ARCHIVE
     *
     * @see .FLAG_DIRECTORY
     *
     * @see .FLAG_HIDDEN
     *
     * @see .FLAG_SYSTEM
     *
     * @see .FLAG_READONLY
     *
     * @see .FLAG_VOLUME_ID
     */
    private fun setFlag(flag: Int) {
        val flags = flags
        data.put(ATTR_OFF, (flag or flags).toByte())
    }

    /**
     * Returns true if a specific flag is currently set.
     *
     * @param flag
     * The flag to be checked.
     * @return True if the flag is set.
     * @see .getFlags
     * @see .setFlag
     * @see .FLAG_ARCHIVE
     *
     * @see .FLAG_DIRECTORY
     *
     * @see .FLAG_HIDDEN
     *
     * @see .FLAG_SYSTEM
     *
     * @see .FLAG_READONLY
     *
     * @see .FLAG_VOLUME_ID
     */
    private fun isFlagSet(flag: Int): Boolean {
        return flags and flag != 0
    }

    /**
     * Sets a mark that indicates that this [.FatDirectoryEntry] shall
     * denote a directory.
     */
    fun setDirectory() {
        setFlag(FLAG_DIRECTORY)
    }

    /**
     * This method extracts the long filename part of the
     * [.FatDirectoryEntry]. It appends the long filename part to the
     * StringBuilder given.
     *
     * @param builder
     * The builder where the long filename part shall be appended.
     * @see .createLfnPart
     * @see .isLfnEntry
     */
    fun extractLfnPart(builder: StringBuilder) {
        val name = CharArray(13)
        name[0] = data.getShort(1).toChar()
        name[1] = data.getShort(3).toChar()
        name[2] = data.getShort(5).toChar()
        name[3] = data.getShort(7).toChar()
        name[4] = data.getShort(9).toChar()
        name[5] = data.getShort(14).toChar()
        name[6] = data.getShort(16).toChar()
        name[7] = data.getShort(18).toChar()
        name[8] = data.getShort(20).toChar()
        name[9] = data.getShort(22).toChar()
        name[10] = data.getShort(24).toChar()
        name[11] = data.getShort(28).toChar()
        name[12] = data.getShort(30).toChar()

        var len = 0
        while (len < 13 && name[len] != '\u0000')
            len++

        builder.append(name, 0, len)
    }

    private fun getUnsignedInt8(offset: Int): Int {
        return data.get(offset).toInt() and 0xff
    }

    private fun getUnsignedInt16(offset: Int): Int {
        val i1 = data.get(offset).toInt() and 0xff
        val i2 = data.get(offset + 1).toInt() and 0xff
        return i2 shl 8 or i1
    }

    private fun getUnsignedInt32(offset: Int): Long {
        val i1 = (data.get(offset).toInt() and 0xff).toLong()
        val i2 = (data.get(offset + 1).toInt() and 0xff).toLong()
        val i3 = (data.get(offset + 2).toInt() and 0xff).toLong()
        val i4 = (data.get(offset + 3).toInt() and 0xff).toLong()
        return i4 shl 24 or (i3 shl 16) or (i2 shl 8) or i1
    }

    private fun setUnsignedInt16(offset: Int, value: Int) {
        data.put(offset, (value and 0xff).toByte())
        data.put(offset + 1, (value.ushr(8) and 0xff).toByte())
    }

    private fun setUnsignedInt32(offset: Int, value: Long) {
        data.put(offset, (value and 0xff).toByte())
        data.put(offset + 1, (value.ushr(8) and 0xff).toByte())
        data.put(offset + 2, (value.ushr(16) and 0xff).toByte())
        data.put(offset + 3, (value.ushr(24) and 0xff).toByte())
    }

    override fun toString(): String {
        return "[FatDirectoryEntry shortName=" + shortName!!.string + "]"
    }

    companion object {

        const val SIZE = 32

        private const val ATTR_OFF = 0x0b
        private const val FILE_SIZE_OFF = 0x1c
        private const val MSB_CLUSTER_OFF = 0x14
        private const val LSB_CLUSTER_OFF = 0x1a
        private const val CREATED_DATE_OFF = 0x10
        private const val CREATED_TIME_OFF = 0x0e
        private const val LAST_WRITE_DATE_OFF = 0x18
        private const val LAST_WRITE_TIME_OFF = 0x16
        private const val LAST_ACCESSED_DATE_OFF = 0x12

        private const val FLAG_READONLY = 0x01
        private const val FLAG_HIDDEN = 0x02
        private const val FLAG_SYSTEM = 0x04
        private const val FLAG_VOLUME_ID = 0x08
        private const val FLAG_DIRECTORY = 0x10
        private const val FLAG_ARCHIVE = 0x20

        private const val SHORTNAME_CASE_OFF = 0x0c

        const val ENTRY_DELETED = 0xe5


        /**
         * Reads a directory [.FatDirectoryEntry] from the given buffer and
         * updates the position of the buffer if successful! Returns
         * `null` if there are no further entries.
         *
         * @param data
         * The buffer where the entries are located.
         * @return Newly constructed entry.
         */
        fun read(data: ByteBuffer): FatDirectoryEntry? {
            val buffer = ByteArray(SIZE)

            if (data.get(data.position()).toInt() == 0)
                return null

            data.get(buffer)

            return FatDirectoryEntry(ByteBuffer.wrap(buffer))
        }

        /**
         * Creates a new [.FatDirectoryEntry] to hold the volume directory
         * in the root directory of a FAT32 file system.
         *
         * @param volumeLabel
         * The volume label.
         * @return Newly constructed entry for the volume label.
         * @see .getVolumeLabel
         * @see .isVolumeLabel
         */
        fun createVolumeLabel(volumeLabel: String): FatDirectoryEntry {
            val result = FatDirectoryEntry()
            val buffer = ByteBuffer.allocate(SIZE)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            System.arraycopy(volumeLabel.toByteArray(Charset.forName("ASCII")), 0, buffer.array(), 0,
                    volumeLabel.length)

            result.data = buffer
            result.setFlag(FLAG_VOLUME_ID)

            return result
        }

        /**
         * This method creates an [.FatDirectoryEntry] for a long file name.
         * Every entry can store up to 13 bytes for the long file name, thus the
         * name has sometimes to be split in more parts.
         *
         * @param unicode
         * The complete unicode String denoting the long filename.
         * @param offset
         * The offset where the part shall begin.
         * @param checksum
         * The checksum of the short name (
         * [ShortName.calculateCheckSum]).
         * @param index
         * The index of this entry, starting at one.
         * @param isLast
         * True if this is the last entry.
         * @return The newly constructed entry holding the lfn part.
         * @see .extractLfnPart
         * @see .isLfnEntry
         */
        fun createLfnPart(unicode: String, offset: Int, checksum: Byte,
                          index: Int, isLast: Boolean): FatDirectoryEntry {
            var unicode = unicode
            var offset = offset
            val result = FatDirectoryEntry()

            if (isLast) {
                val diff = unicode.length - offset
                if (diff < 13) {
                    val builder = StringBuilder(13)
                    builder.append(unicode, offset, unicode.length)
                    // end mark
                    builder.append('\u0000')

                    // fill with 0xffff
                    for (i in 0 until 13 - diff) {
                        builder.append(0xffff.toChar())
                    }

                    offset = 0
                    unicode = builder.toString()
                }
            }

            val buffer = ByteBuffer.allocate(SIZE)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            buffer.put(0, (if (isLast) index + (1 shl 6) else index).toByte())
            buffer.putShort(1, unicode[offset].toShort())
            buffer.putShort(3, unicode[offset + 1].toShort())
            buffer.putShort(5, unicode[offset + 2].toShort())
            buffer.putShort(7, unicode[offset + 3].toShort())
            buffer.putShort(9, unicode[offset + 4].toShort())
            // Special mark for lfn entry
            buffer.put(11, (FLAG_HIDDEN or FLAG_VOLUME_ID or FLAG_READONLY or FLAG_SYSTEM).toByte())
            // unused
            buffer.put(12, 0.toByte())
            buffer.put(13, checksum)
            buffer.putShort(14, unicode[offset + 5].toShort())
            buffer.putShort(16, unicode[offset + 6].toShort())
            buffer.putShort(18, unicode[offset + 7].toShort())
            buffer.putShort(20, unicode[offset + 8].toShort())
            buffer.putShort(22, unicode[offset + 9].toShort())
            buffer.putShort(24, unicode[offset + 10].toShort())
            // unused
            buffer.putShort(26, 0.toShort())
            buffer.putShort(28, unicode[offset + 11].toShort())
            buffer.putShort(30, unicode[offset + 12].toShort())

            result.data = buffer

            return result
        }

        /**
         * This method decodes a timestamp from an [.FatDirectoryEntry].
         *
         * @param date
         * The data of the entry.
         * @param time
         * The time of the entry.
         * @return Time in milliseconds since January 1 00:00:00, 1970 UTC
         */
        private fun decodeDateTime(date: Int, time: Int): Long {
            val calendar = Calendar.getInstance(FileSystemFactory.timeZone)

            calendar.set(Calendar.YEAR, 1980 + (date shr 9))
            calendar.set(Calendar.MONTH, (date shr 5 and 0x0f) - 1)
            calendar.set(Calendar.DAY_OF_MONTH, date and 0x1f)
            calendar.set(Calendar.HOUR_OF_DAY, time shr 11)
            calendar.set(Calendar.MINUTE, time shr 5 and 0x3f)
            calendar.set(Calendar.SECOND, (time and 0x1f) * 2)

            return calendar.timeInMillis
        }

        /**
         * This method encodes the date given to a timestamp suitable for an
         * [.FatDirectoryEntry].
         *
         * @param timeInMillis
         * Time in milliseconds since January 1 00:00:00, 1970 UTC.
         * @return The date suitable to store in an #[FatDirectoryEntry].
         */
        private fun encodeDate(timeInMillis: Long): Int {
            val calendar = Calendar.getInstance(FileSystemFactory.timeZone)

            calendar.timeInMillis = timeInMillis

            return ((calendar.get(Calendar.YEAR) - 1980 shl 9)
                    + (calendar.get(Calendar.MONTH) + 1 shl 5) + calendar.get(Calendar.DAY_OF_MONTH))

        }

        /**
         * This method encodes the time given to a timestamp suitable for an
         * [.FatDirectoryEntry].
         *
         * @param timeInMillis
         * Time in milliseconds since January 1 00:00:00, 1970 UTC.
         * @return The time suitable to store in an #[FatDirectoryEntry].
         */
        private fun encodeTime(timeInMillis: Long): Int {
            val calendar = Calendar.getInstance(FileSystemFactory.timeZone)

            calendar.timeInMillis = timeInMillis

            return ((calendar.get(Calendar.HOUR_OF_DAY) shl 11) + (calendar.get(Calendar.MINUTE) shl 5)
                    + calendar.get(Calendar.SECOND) / 2)

        }
    }
}
