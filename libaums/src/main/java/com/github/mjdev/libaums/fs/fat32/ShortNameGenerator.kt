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

package com.github.mjdev.libaums.fs.fat32

import java.util.Locale

/**
 * This class is responsible for generating valid 8.3 short names for any given
 * long file name.
 *
 * @author mjahnen
 * @see FatLfnDirectoryEntry
 *
 * @see FatDirectoryEntry
 */
/* package */internal object ShortNameGenerator {

    /**
     * See fatgen103.pdf from Microsoft for allowed characters.
     *
     * @param c
     * The character to test.
     * @return True if the character is allowed in an 8.3 short name.
     */
    private fun isValidChar(c: Char): Boolean {
        if (c >= '0' && c <= '9')
            return true
        return if (c >= 'A' && c <= 'Z') true else c == '$' || c == '%' || c == '\'' || c == '-' || c == '_' || c == '@' || c == '~'
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
    fun getNextHexPart(hexPart: String, limit: Int): String? {
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
    /* package */ fun generateShortName(lfnName: String,
                                        existingShortNames: Collection<ShortName>): ShortName {
        var lfnName = lfnName
        lfnName = lfnName.toUpperCase(Locale.ROOT).trim { it <= ' ' }

        // remove leading periods
        var i: Int
        i = 0
        while (i < lfnName.length) {
            if (lfnName[i] != '.')
                break
            i++
        }

        lfnName = lfnName.substring(i)
        lfnName = lfnName.replace(" ", "")

        var filenamePart = ""
        var extensionPart = ""

        val indexOfDot = lfnName.lastIndexOf(".")
        if (indexOfDot == -1) {
            // no extension
            filenamePart = lfnName
            extensionPart = ""

        } else {
            // has extension
            filenamePart = lfnName.substring(0, indexOfDot)
            extensionPart = lfnName.substring(indexOfDot + 1)
            if (extensionPart.length > 3) {
                extensionPart = extensionPart.substring(0, 3)
            }
        }

        // remove invalid chars
        if (containsInvalidChars(filenamePart)) {
            filenamePart = replaceInvalidChars(filenamePart)
        }

        // remove invalid chars
        if (containsInvalidChars(extensionPart)) {
            extensionPart = replaceInvalidChars(extensionPart)
        }

        var filePrefix = filenamePart
        if (filenamePart.length == 0) {
            filePrefix = "__"
        } else if (filenamePart.length == 1) {
            filePrefix = filePrefix + "_"
        } else if (filenamePart.length == 2) {
            // Do nothing
        } else if (filenamePart.length > 2) {
            filePrefix = filenamePart.substring(0, 2)
        }

        var extSuffix = extensionPart
        // The extensionPart must be at least 1 here
        if (extensionPart.length == 0) {
            extSuffix = "000"
        } else if (extensionPart.length == 1) {
            extSuffix = extensionPart + "00"
        } else if (extensionPart.length == 2) {
            extSuffix = extensionPart + "0"
        }

        var hexPart: String? = "0000"
        var tildeDigit = 0

        var result = ShortName("$filePrefix$hexPart~$tildeDigit", extSuffix)
        while (containShortName(existingShortNames, result)) {
            if (getNextHexPart(hexPart, 4) != null) {
                hexPart = getNextHexPart(hexPart, 4)
            } else {
                if (tildeDigit + 1 < 10) {
                    tildeDigit += 1
                    hexPart = "0000"
                } else {
                    // This should not happen
                    break
                }
            }// HexPart is used up
            result = ShortName("$filePrefix$hexPart~$tildeDigit", extSuffix)
        }

        return result
    }

    fun containShortName(shortNames: Collection<ShortName>, shortName: ShortName): Boolean {
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
