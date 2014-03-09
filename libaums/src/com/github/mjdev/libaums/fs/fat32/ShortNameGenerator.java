package com.github.mjdev.libaums.fs.fat32;


import java.util.Collection;
import java.util.Locale;

/* package */ class ShortNameGenerator {
	
	/**
	 * See fatgen103.pdf from microsoft for allowed characters
	 */
	private static boolean isValidChar(char c) {
        if (c >= '0' && c <= '9') return true;
        if (c >= 'A' && c <= 'Z') return true;
        
        return (c == '$' || c == '%' || c == '\'' || c == '-' || c == '_' || c == '@' ||
                c == '~' || c == '`' || c == '!'  || c == '(' || c == ')' || c == '{' ||
                c == '}' || c == '^' || c == '#'  || c == '&');
    }
	
	private static boolean containsInvalidChars(String str) {
		int length = str.length();
		for(int i = 0; i < length; i++) {
			final char c = str.charAt(i);
			if(!isValidChar(c)) return true;
		}
		return false;
	}
	
	private static String replaceInvalidChars(String str) {
		int length = str.length();
		StringBuilder builder = new StringBuilder(length);
		
		for(int i = 0; i < length; i++) {
			final char c = str.charAt(i);
			if(isValidChar(c)) {
				builder.append(c);
			} else {
				builder.append("_");
			}
		}
		
		return builder.toString();
	}
	
	/* package */ static ShortName generateShortName(String lfnName, Collection<ShortName> existingShortNames) {
		lfnName = lfnName.toUpperCase(Locale.ROOT).trim();
		
		// remove leading periods
		int i;
		for(i = 0; i < lfnName.length(); i++) {
			if(lfnName.charAt(i) != '.') break;
		}
		
		lfnName = lfnName.substring(i);
		
		final int periodIndex = lfnName.lastIndexOf('.');
		String name;
		String extension;
		boolean losslyConversion = false;
		
		if(periodIndex == -1) {
			if(containsInvalidChars(lfnName)) {
				losslyConversion = true;
				name = replaceInvalidChars(lfnName);
			} else {
				name = lfnName;
			}
			extension = "";
		} else {
			String tmp = lfnName.substring(0, periodIndex);
			if(containsInvalidChars(tmp)) {
				losslyConversion = true;
				name = replaceInvalidChars(lfnName);
			} else {
				name = tmp;
			}
			
			extension = replaceInvalidChars(lfnName.substring(periodIndex + 1));
			if(extension.length() > 3) {
				extension = extension.substring(0, 3);
			}
		}
		
		name = name.replace(" ", "");
		extension = extension.replace(" ", "");
		
		ShortName result = new ShortName(name, extension);
		
		if(losslyConversion || name.length() > 8 || existingShortNames.contains(result)) {
			int maxLen = Math.min(name.length(), 8);
			for(i = 1; i < 999999; i++) {
				final String suffix = "~" + i;
				final int suffixLen = suffix.length();
				final String newName = name.substring(0, Math.min(maxLen, 8 - suffixLen)) + suffix;
				result = new ShortName(newName, extension);
				
				if(!existingShortNames.contains(result)) break;
			}
		}
		
		return result;
	}
	
}
