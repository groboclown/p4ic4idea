package com.perforce.p4java.common.base;

import static com.perforce.p4java.common.base.Preconditions.checkPositionIndexes;

/**
 * @author Sean Shou
 * @since 10/11/2016
 */
public final class Utf8Utils {
    private Utf8Utils() { /* util */ }

    public static boolean isWellFormed(byte[] bytes) {
        return isWellFormed(bytes, 0, bytes.length);
    }


    public static boolean isWellFormed(byte[] bytes, int off, int len) {
        int end = off + len;
        checkPositionIndexes(off, end, bytes.length);
        // Look for the first non-ASCII character.
        for (int i = off; i < end; i++) {
            if (bytes[i] < 0) {
                return isWellFormedSlowPath(bytes, i, end);
            }
        }
        return true;
    }

    private static boolean isWellFormedSlowPath(byte[] bytes, int off, int end) {
        int index = off;
        while (true) {
            int byte1;

            // Optimize for interior runs of ASCII bytes.
            do {
                if (index >= end) {
                    return true;
                }
            } while ((byte1 = bytes[index++]) >= 0);

            if (byte1 < (byte) 0xE0) {
                // Two-byte form.
                if (index == end) {
                    return false;
                }
                // Simultaneously check for illegal trailing-byte in leading position
                // and overlong 2-byte form.
                if (byte1 < (byte) 0xC2 || bytes[index++] > (byte) 0xBF) {
                    return false;
                }
            } else if (byte1 < (byte) 0xF0) {
                // Three-byte form.
                if (index + 1 >= end) {
                    return false;
                }
                int byte2 = bytes[index++];
                if (byte2 > (byte) 0xBF
                        // Overlong? 5 most significant bits must not all be zero.
                        || (byte1 == (byte) 0xE0 && byte2 < (byte) 0xA0)
                        // Check for illegal surrogate codepoints.
                        || (byte1 == (byte) 0xED && (byte) 0xA0 <= byte2)
                        // Third byte trailing-byte test.
                        || bytes[index++] > (byte) 0xBF) {
                    return false;
                }
            } else {
                // Four-byte form.
                if (index + 2 >= end) {
                    return false;
                }
                int byte2 = bytes[index++];
                if (byte2 > (byte) 0xBF
                        // Check that 1 <= plane <= 16. Tricky optimized form of:
                        // if (byte1 > (byte) 0xF4
                        //     || byte1 == (byte) 0xF0 && byte2 < (byte) 0x90
                        //     || byte1 == (byte) 0xF4 && byte2 > (byte) 0x8F)
                        || (((byte1 << 28) + (byte2 - (byte) 0x90)) >> 30) != 0
                        // Third byte trailing-byte test
                        || bytes[index++] > (byte) 0xBF
                        // Fourth byte trailing-byte test
                        || bytes[index++] > (byte) 0xBF) {
                    return false;
                }
            }
        }
    }
}
