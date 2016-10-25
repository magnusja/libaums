package com.github.mjdev.libaums.util;

/**
 * Created by Yuriy on 25.10.2016.
 */

public class PrettyPrint {

    /* Each line in the output has the offset, as a hex value, in the
    * first four columns, followed by " - " (3 columns), followed by
    * the hex values (BYTES_IN_ROW*3 columns), followed by "  "
    * (2 columns), followed by the printable ASCII values.
    */
    private static final int BYTES_IN_ROW = 16;
    private static final int OFFSET_SIZE = 4;
    private static final int SEPARATOR1_SIZE = 3;
    private static final int SEPARATOR2_SIZE = 2;
    private static final int ASCII_START = OFFSET_SIZE + SEPARATOR1_SIZE +
            (BYTES_IN_ROW * 3) + SEPARATOR2_SIZE;
    private static final char PRINTABLE_LOW = ' ';  // 0x20
    private static final char PRINTABLE_HIGH = '~'; // 0x7e

    private static final char[] buffer = new char[ASCII_START + BYTES_IN_ROW];

    /* Hexadecimal digits. */
    private static final char[] hc = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Converts a byte array to a pretty printed string of the kind
     * shown below
     * <pre>
     * 0000 - 16 03 00 00 38 ee ba df-fa fa 64 0c 45 5e 11 e3   ....8.....d.E^..
     * 0010 - 5a 0f 11 33 48 23 d8 02-ad 17 9b 45 03 dd f6 7d   Z..3H#.....E...}
     * 0020 - 88 91 d4 2c e1 2e 78 da-5a 6f 2c 39 98 0e 38 d5   ...,..x.Zo,9..8.
     * 0030 - bb 29                                             .)
     * </pre>
     *
     * @param b byte array containing the bytes to be converted
     * @return a pretty printed string of corresponding hexadecimal
     * and printable ASCII values.
     */
    public static String prettyPrint(byte[] b) {
        return prettyPrint(b, 0, b.length);
    }

    /**
     * Converts a subsequence of bytes in a byte array into a
     * pretty printed string of the kind shown below
     * <pre>
     * 0000 - 16 03 00 00 38 ee ba df-fa fa 64 0c 45 5e 11 e3   ....8.....d.E^..
     * 0010 - 5a 0f 11 33 48 23 d8 02-ad 17 9b 45 03 dd f6 7d   Z..3H#.....E...}
     * 0020 - 88 91 d4 2c e1 2e 78 da-5a 6f 2c 39 98 0e 38 d5   ...,..x.Zo,9..8.
     * 0030 - bb 29                                             .)
     * </pre>
     *
     * @param b   byte array containing the bytes to be converted
     * @param off starting offset of the byte subsequence inside b
     * @param len number of bytes to be converted
     * @return a pretty printed string of corresponding hexadecimal
     * and printable ASCII values.
     */
    public static String prettyPrint(byte[] b, int off, int len) {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, b, off, len);
        return sb.toString();
    }

    /**
     * Appends a subsequence of bytes in a byte array into a
     * pretty printed StringBuffer of the kind shown below
     * <pre>
     * 0000 - 16 03 00 00 38 ee ba df-fa fa 64 0c 45 5e 11 e3   ....8.....d.E^..
     * 0010 - 5a 0f 11 33 48 23 d8 02-ad 17 9b 45 03 dd f6 7d   Z..3H#.....E...}
     * 0020 - 88 91 d4 2c e1 2e 78 da-5a 6f 2c 39 98 0e 38 d5   ...,..x.Zo,9..8.
     * 0030 - bb 29                                             .)
     * </pre>
     *
     * @param buf StringBuffer to append the pretty printing to
     * @param b   byte array containing the bytes to be converted
     * @param off starting offset of the byte subsequence inside b
     * @param len number of bytes to be converted
     * @return the StringBuffer with the appended pretty printed hexadecimal
     * and printable ASCII values.
     */
    public static StringBuffer prettyPrint(StringBuffer buf, byte[] b, int off, int len) {
        int rows = len / BYTES_IN_ROW;

        // Deal with each complete group of BYTES_IN_ROW bytes
        boolean lastLineIsZero = false;
        for (int i = 0; i < rows; i++) {
            char[] line = prettyPrintToCharArray(b, off + i * BYTES_IN_ROW, BYTES_IN_ROW, true);
            if (line == null && lastLineIsZero) {
                continue;
            } else if (line != null) {
                lastLineIsZero = false;
                buf.append(line);
                buf.append('\n');
            } else {
                lastLineIsZero = true;
            }
        }

        // Deal with the last incomplete group, if any
        lastLineIsZero = false;
        if ((len % BYTES_IN_ROW) != 0) {
            char[] line = prettyPrintToCharArray(b, off + rows * BYTES_IN_ROW, (len % BYTES_IN_ROW), false);
            buf.append(line);
            buf.append('\n');
        }
        return buf;
    }

    /*
     * Prints at most BYTES_IN_ROW bytes at a time, i.e. 0 < len <= BYTES_IN_ROW
     * and offset must be a multiple of BYTES_IN_ROW
     */
    private static char[] prettyPrintToCharArray(byte[] b, int off, int len, boolean reduce) {
        char[] r = buffer;          // = new char[ASCII_START + BYTES_IN_ROW];
        int byteVal = 0;
        int j = 0;

        // Initialize with spaces
        for (int i = 0; i < r.length; i++) r[i] = ' ';

        // Print the starting offset in hex (4 columns)
        for (int i = 1; i <= OFFSET_SIZE; i++) {
            r[j++] = hc[(off >> 4 * (OFFSET_SIZE - i)) & 0x0f];
        }

        // separator (3 columns)
        r[j++] = ' ';
        r[j++] = '-';
        r[j++] = ' ';

        boolean onlyZero = true;
        // Print the hex values and printable ASCII characters
        for (int i = 0; i < len; i++) {
            byteVal = b[off + i] & 0xff;
            // hex values (BYTES_IN_ROW*3 = 48 columns)
            r[j++] = hc[byteVal >> 4];
            r[j++] = hc[byteVal & 0x0f];
            // print something other than ' ' at halfway mark for readability
            if (i == ((BYTES_IN_ROW + 1) / 2) - 1) {
                r[j++] = '-';
            } else {
                r[j++] = ' ';
            }

            // ASCII values ...
            if ((byteVal < (byte) PRINTABLE_LOW) ||
                    (byteVal > (byte) PRINTABLE_HIGH)) {
                r[ASCII_START + i] = '.';
            } else {
                r[ASCII_START + i] = (char) (byteVal);
            }
            onlyZero = (byteVal != 0) ? false : onlyZero;
        }

        return (onlyZero && reduce) ? null : r;
    }
}
