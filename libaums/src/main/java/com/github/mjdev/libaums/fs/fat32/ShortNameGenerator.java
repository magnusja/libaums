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

package com.github.mjdev.libaums.fs.fat32;

import java.util.Collection;
import java.util.Locale;

/**
 * This class is responsible for generating valid 8.3 short names for any given
 * long file name.
 * 
 * @author mjahnen
 * @see FatLfnDirectoryEntry
 * @see FatDirectoryEntry
 */
/* package */class ShortNameGenerator {

	/**
	 * See fatgen103.pdf from Microsoft for allowed characters.
	 * 
	 * @param c
	 *            The character to test.
	 * @return True if the character is allowed in an 8.3 short name.
	 */
	private static boolean isValidChar(char c) {
		if (c >= '0' && c <= '9')
			return true;
		if (c >= 'A' && c <= 'Z')
			return true;

		return (c == '$' || c == '%' || c == '\'' || c == '-' || c == '_' || c == '@' || c == '~'
				|| c == '`' || c == '!' || c == '(' || c == ')' || c == '{' || c == '}' || c == '^'
				|| c == '#' || c == '&');
	}

	/**
	 * 
	 * @param str
	 *            The String to test.
	 * @return True if the String contains any invalid chars which are not
	 *         allowed on 8.3 short names.
	 */
	private static boolean containsInvalidChars(String str) {
		int length = str.length();
		for (int i = 0; i < length; i++) {
			final char c = str.charAt(i);
			if (!isValidChar(c))
				return true;
		}
		return false;
	}

	/**
	 * Replaces all invalid characters in an string with an underscore (_).
	 * 
	 * @param str
	 *            The string where invalid chars shall be replaced.
	 * @return The new string only containing valid chars.
	 */
	private static String replaceInvalidChars(String str) {
		int length = str.length();
		StringBuilder builder = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			final char c = str.charAt(i);
			if (isValidChar(c)) {
				builder.append(c);
			} else {
				builder.append("_");
			}
		}

		return builder.toString();
	}

	/**
	 * Generate the next possible hex part for using in SFN. We are using a
	 * similar approach as what Windows 2000 did.
	 */
	static String getNextHexPart(String hexPart, int limit) {
		long hexValue = Long.parseLong(hexPart, 16);
		hexValue += 1;
		String tempHexString = Long.toHexString(hexValue);
		if (tempHexString.length() <= limit) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < limit - tempHexString.length(); i++) {
				sb.append("0");
			}
			return sb.toString() + tempHexString;
		}
		return null;
	}

	/**
	 * Generates an 8.3 short name for a given long file name. It creates a
	 * suffix at the end of the short name if there is already a existing entry
	 * in the directory with an equal short name.
	 * 
	 * @param lfnName
	 *            Long file name.
	 * @param existingShortNames
	 *            The short names already existing in the directory.
	 * @return The generated short name.
	 */
	/* package */static ShortName generateShortName(String lfnName,
			Collection<ShortName> existingShortNames) {
		lfnName = lfnName.toUpperCase(Locale.ROOT).trim();

		// remove leading periods
		int i;
		for (i = 0; i < lfnName.length(); i++) {
			if (lfnName.charAt(i) != '.')
				break;
		}

		lfnName = lfnName.substring(i);
		lfnName = lfnName.replace(" ", "");

		String filenamePart = "";
		String extensionPart = "";

		int indexOfDot = lfnName.lastIndexOf(".");
		if (indexOfDot == -1) {
			// no extension
			filenamePart = lfnName;
			extensionPart = "";

		} else {
			// has extension
			filenamePart = lfnName.substring(0, indexOfDot);
			extensionPart = lfnName.substring(indexOfDot + 1);
			if (extensionPart.length() > 3) {
				extensionPart = extensionPart.substring(0, 3);
			}
		}

		// remove invalid chars
		if (containsInvalidChars(filenamePart)) {
			filenamePart = replaceInvalidChars(filenamePart);
		}

		// remove invalid chars
		if (containsInvalidChars(extensionPart)) {
			extensionPart = replaceInvalidChars(extensionPart);
		}

		String filePrefix = filenamePart;
		if (filenamePart.length() == 0) {
			filePrefix = "__";
		} else if (filenamePart.length() == 1) {
			filePrefix = filePrefix + "_";
		} else if (filenamePart.length() == 2) {
			// Do nothing
		} else if (filenamePart.length() > 2) {
			filePrefix = filenamePart.substring(0, 2);
		}

		String extSuffix = extensionPart;
		// The extensionPart must be at least 1 here
		if (extensionPart.length() == 0) {
			extSuffix = "000";
		} else if (extensionPart.length() == 1) {
			extSuffix = extensionPart + "00";
		} else if (extensionPart.length() == 2) {
			extSuffix = extensionPart + "0";
		}

		String hexPart = "0000";
		int tildeDigit = 0;

		ShortName result = new ShortName(filePrefix + hexPart + "~" + tildeDigit, extSuffix);
		while (containShortName(existingShortNames, result)) {
			if (getNextHexPart(hexPart, 4) != null) {
				hexPart = getNextHexPart(hexPart, 4);
			}
			// HexPart is used up
			else {
				if (tildeDigit + 1 < 10) {
					tildeDigit += 1;
					hexPart = "0000";
				} else {
					// This should not happen
					break;
				}
			}
			result = new ShortName(filePrefix + hexPart + "~" + tildeDigit, extSuffix);
		}

		return result;
	}

	public static boolean containShortName(Collection<ShortName> shortNames, ShortName shortName) {
		boolean contain = false;
		for (ShortName temp : shortNames) {
			if (temp.getString().equalsIgnoreCase(shortName.getString())) {
				contain = true;
				break;
			}
		}
		return contain;
	}
}
