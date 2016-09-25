package com.github.mjdev.libaums.util;

import com.github.mjdev.libaums.fs.BootSector;

/**
 * $Id$
 * <p>
 * Copyright (C) 2003-2015 JNode.org
 * <p>
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

public class FatUtils {
    public static final int FIRST_CLUSTER = 2;

    /**
     * Gets the offset (in bytes) of the fat with the given index
     *
     * @param bs
     * @param fatNr (0..)
     * @return long
     */
    public static long getFatOffset(BootSector bs, int fatNr) {
        long sectSize = bs.getBytesPerSector();
        long sectsPerFat = bs.getSectorsPerFat();
        long resSects = bs.getReservedSectors();
        long offset = resSects * sectSize;
        long fatSize = sectsPerFat * sectSize;
        offset += fatNr * fatSize;

        return offset;
    }

    /**
     * Gets the offset (in bytes) of the root directory with the given index
     *
     * @param bs
     * @return long
     */
    public static long getRootDirOffset(BootSector bs) {
        long sectSize = bs.getBytesPerSector();
        long sectsPerFat = bs.getSectorsPerFat();
        int fats = bs.getFatCount();
        long offset = getFatOffset(bs, 0);
        offset += fats * sectsPerFat * sectSize;
        return offset;
    }

    /**
     * Gets the offset of the data (file) area
     *
     * @param bs
     * @return long
     */
    public static long getFilesOffset(BootSector bs) {
        long offset = getRootDirOffset(bs);
        offset += bs.getNumberRootDirEntries() * 32;
        return offset;
    }

    /**
     * Return the name (without extension) of a full file name
     *
     * @param nameExt
     * @return the name part
     */
    public static String splitName(String nameExt) {
        int i = nameExt.indexOf('.');
        if (i < 0) {
            return nameExt;
        } else {
            return nameExt.substring(0, i);
        }
    }

    /**
     * Return the extension (without name) of a full file name
     *
     * @param nameExt
     * @return the extension part
     */
    public static String splitExt(String nameExt) {
        int i = nameExt.indexOf('.');
        if (i < 0) {
            return "";
        } else {
            return nameExt.substring(i + 1);
        }
    }

    /**
     * Normalize full file name in DOS 8.3 format from the name and the ext
     *
     * @param name a DOS 8 name
     * @param ext  a DOS 3 extension
     * @return the normalized DOS 8.3 name
     */
    public static String normalizeName(String name, String ext) {
        if (ext.length() > 0) {
            return (name + "." + ext).toUpperCase();
        } else {
            return name.toUpperCase();
        }
    }

    /**
     * Normalize full file name in DOS 8.3 format from the given full name
     *
     * @param nameExt a DOS 8.3 name + extension
     * @return the normalized DOS 8.3 name
     */
    public static String normalizeName(String nameExt) {
        if (nameExt.equals("."))
            return nameExt;

        if (nameExt.equals(".."))
            return nameExt;

        return normalizeName(splitName(nameExt), splitExt(nameExt));
    }

    public static void checkValidName(String name) {
        checkString(name, "name", 1, 8);
    }

    public static void checkValidExt(String ext) {
        checkString(ext, "extension", 0, 3);
    }

    private static void checkString(String str, String strType, int minLength, int maxLength) {
        if (str == null)
            throw new IllegalArgumentException(strType + " is null");
        if (str.length() < minLength)
            throw new IllegalArgumentException(strType + " must have at least " + maxLength +
                    " characters: " + str);
        if (str.length() > maxLength)
            throw new IllegalArgumentException(strType + " has more than " + maxLength +
                    " characters: " + str);
    }

    public static final int SUBNAME_SIZE = 13;

    public static byte getOrdinal(byte[] rawData, int offset) {
        return (byte) LittleEndian.getUInt8(rawData, offset);
    }

    public static byte getCheckSum(byte[] rawData, int offset) {
        return (byte) LittleEndian.getUInt8(rawData, offset + 13);
    }
}
