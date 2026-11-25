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

package me.jahnen.libaums.core.fs.fat32

import java.util.*

/**
 * This class is responsible for generating valid 8.3 short names for any given
 * long file name.
 *
 * @author mjahnen
 * @see FatLfnDirectoryEntry
 *
 * @see FatDirectoryEntry
 */
internal object ShortNameGenerator {

    /**
     * See fatgen103.pdf from Microsoft for allowed characters.
     *
     * @param c
     * The character to test.
     * @return True if the character is allowed in an 8.3 short name.
     */
    private fun isValidChar(c: Char): Boolean {
        if (c in '0'..'9')
            return true
        return if (c in 'A'..'Z') true else c == '$' || c == '%' || c == '\'' || c == '-' || c == '_' || c == '@' || c == '~'
                || c == '`' || c == '!' || c == '(' || c == ')' || c == '{' || c == '}' || c == '^'
                || c == '#' || c == '&'

    }

    /**
     *
     * @param str
     * The String to test.
     * @return True if the String contains any invalid chars which are not
     * allowed on 8.3 short names.
     */
    private fun containsInvalidChars(str: String): Boolean {
        val length = str.length
        for (i in 0 until length) {
            val c = str[i]
            if (!isValidChar(c))
                return true
        }
        return false
    }

    /**
     * Replaces all invalid characters in an string with an underscore (_).
     *
     * @param str
     * The string where invalid chars shall be replaced.
     * @return The new string only containing valid chars.
     */
    private fun replaceInvalidChars(str: String): String {
        val length = str.length
        val builder = StringBuilder(length)

        for (i in 0 until length) {
            val c = str[i]
            if (isValidChar(c)) {
                builder.append(c)
            } else {
                builder.append("_")
            }
        }

        return builder.toString()
    }

    /**
     * Generate the next possible hex part for using in SFN. We are using a
     * similar approach as what Windows 2000 did.
     */
    private fun getNextHexPart(hexPart: String, limit: Int): String? {
        var hexValue = java.lang.Long.parseLong(hexPart, 16)
        hexValue += 1
        val tempHexString = java.lang.Long.toHexString(hexValue)
        if (tempHexString.length <= limit) {
            val sb = StringBuilder()
            for (i in 0 until limit - tempHexString.length) {
                sb.append("0")
            }
            return sb.toString() + tempHexString
        }
        return null
    }

    /**
     * Generates an 8.3 short name for a given long file name. It creates a
     * suffix at the end of the short name if there is already a existing entry
     * in the directory with an equal short name.
     *
     * @param lfnName
     * Long file name.
     * @param existingShortNames
     * The short names already existing in the directory.
     * @return The generated short name.
     */
    fun generateShortName(lfnName: String,
                                        existingShortNames: Collection<ShortName>): ShortName {
        var lfnName = lfnName
        lfnName = lfnName.toUpperCase(Locale.ROOT).trim { it <= ' ' }

        // remove leading periods
        var i = 0
        while (i < lfnName.length) {
            if (lfnName[i] != '.')
                break
            i++
        }

        lfnName = lfnName.substring(i)
        lfnName = lfnName.replace(" ", "")

        var filenamePart: String
        var extensionPart: String

        val indexOfDot = lfnName.lastIndexOf(".")
        // Multiple dots (e.g. "a.b.txt") are themselves a reason to mangle on real Windows, same
        // as the dot found NOT being the only one. Track that before splitting on the LAST dot.
        val hasEmbeddedDot = indexOfDot != -1 && lfnName.substring(0, indexOfDot).contains(".")
        if (indexOfDot == -1) {
            // no extension
            filenamePart = lfnName
            extensionPart = ""

        } else {
            // has extension
            filenamePart = lfnName.substring(0, indexOfDot)
            extensionPart = lfnName.substring(indexOfDot + 1)
        }

        // An extension over 3 chars (.flac, .aiff, .jpeg, .html, ...) must be truncated for the
        // *mangled* 8.3 extension field, but that truncation is itself real information loss a
        // genuine Windows-style writer would never paper over with an unmangled name — so it has
        // to disqualify the fast path below, checked against the ORIGINAL length, before any
        // truncation happens.
        val extensionTooLong = extensionPart.length > 3
        if (extensionTooLong) {
            extensionPart = extensionPart.substring(0, 3)
        }

        // remove invalid chars, but remember whether anything actually changed — a replaced char
        // is itself real information loss, same reasoning as the extension truncation above.
        val originalFilenamePart = filenamePart
        if (containsInvalidChars(filenamePart)) {
            filenamePart = replaceInvalidChars(filenamePart)
        }

        val originalExtensionPart = extensionPart
        if (containsInvalidChars(extensionPart)) {
            extensionPart = replaceInvalidChars(extensionPart)
        }

        // Real VFAT writers (Windows, macOS) only generate a mangled "XXnnnn~n" short name when
        // the long name doesn't already fit cleanly in 8.3 (too long, invalid chars, multiple
        // dots) or collides with an existing entry. This implementation used to always mangle,
        // which produces a syntactically valid but non-standard short name — e.g. "EXPORT.PDB"
        // (fits 8.3 perfectly) became "EX0000~0.PDB". That's not just cosmetic: a real Pioneer
        // CDJ/XDJ was confirmed (2026-11-24, live hardware) to do a direct short-name lookup for
        // its critical files and silently fail to find them — "rekordbox database not found" —
        // even though a long-name-aware reader resolves the same entry fine. Skip the fast path
        // the exact same way Windows does: use the name as-is, uppercased, when it already fits
        // — i.e. nothing was truncated, no char was replaced, no embedded dot, and the result is
        // non-empty (an empty short name is invalid regardless of how it arose).
        val fitsCleanly = !extensionTooLong && !hasEmbeddedDot &&
            filenamePart == originalFilenamePart && extensionPart == originalExtensionPart &&
            filenamePart.isNotEmpty() && filenamePart.length <= 8 && extensionPart.length <= 3
        if (fitsCleanly) {
            val direct = ShortName(filenamePart, extensionPart)
            if (!containShortName(existingShortNames, direct)) {
                return direct
            }
        }

        var filePrefix = filenamePart
        when {
            filenamePart.isEmpty() -> filePrefix = "__"
            filenamePart.length == 1 -> filePrefix += "_"
            filenamePart.length == 2 -> {
                // Do nothing
            }
            filenamePart.length > 2 -> filePrefix = filenamePart.substring(0, 2)
        }

        var extSuffix = extensionPart
        when {
            extensionPart.isEmpty() -> extSuffix = "000"
            extensionPart.length == 1 -> extSuffix = extensionPart + "00"
            extensionPart.length == 2 -> extSuffix = extensionPart + "0"
        }

        var hexPart = "0000"
        var tildeDigit = 0

        var result = ShortName("$filePrefix$hexPart~$tildeDigit", extSuffix)
        while (containShortName(existingShortNames, result)) {
            val hexPartNullable = getNextHexPart(hexPart, 4)
            if (hexPartNullable != null) {
                hexPart = hexPartNullable
            } else {
                if (tildeDigit + 1 < 10) {
                    tildeDigit += 1
                    hexPart = "0000"
                } else {
                    // This should not happen
                    break
                }
            }
            result = ShortName("$filePrefix$hexPart~$tildeDigit", extSuffix)
        }

        return result
    }

    private fun containShortName(shortNames: Collection<ShortName>, shortName: ShortName): Boolean {
        var contain = false
        for (temp in shortNames) {
            if (temp.string.equals(shortName.string, ignoreCase = true)) {
                contain = true
                break
            }
        }
        return contain
    }
}
