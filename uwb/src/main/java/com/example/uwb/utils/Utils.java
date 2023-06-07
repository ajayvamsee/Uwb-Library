package com.example.uwb.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 05:30
 */
public class Utils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Encodes a byte array into a hexadecimal string
     *
     * @param bytes Input byte array
     * @return The resultant string in hexadecimal format.
     */
    public static String byteArrayToHexString(byte[] bytes, int length) {
        char[] hexChars = new char[length * 2];
        for (int j = 0; j < length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Encodes a byte array into a hexadecimal string
     *
     * @param bytes Input byte array
     * @return The resultant string in hexadecimal format.
     */
    public static String byteArrayToHexString(byte[] bytes) {
        return byteArrayToHexString(bytes, bytes.length);
    }

    /**
     * Encodes an input string in hexadecimal format (0-9, A-F) to an array of bytes
     *
     * @param hexString Input string in hexadecimal value
     * @return Resultant array of bytes
     */
    public static byte[] hexStringtoByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Encodes a byte array into a ASCII string
     *
     * @param bytes Input byte array
     * @return ASCII string representation of the specified bytes.
     * @throws UnsupportedEncodingException US-ASCII charset name used
     */
    public static String byteArrayToAsciiString(byte[] bytes) {
        String ascii = null;
        if (bytes != null && !arrayIsAllZeros(bytes)) {
            ascii = new String(bytes, StandardCharsets.US_ASCII);
            // Check the characters
            char[] charArray = ascii.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                // Show null character as blank space
                if (charArray[i] == (char) 0x00) {
                    charArray[i] = ' ';
                }
            }

            ascii = new String(charArray);
        }

        return ascii;
    }

    /**
     * Convert the byte array to an int
     *
     * @param b The byte array
     * @return The integer
     */
    public static int byteArrayToInt(byte[] b) {
        if (b.length == 1) {
            return b[0] & 0xFF;
        } else if (b.length == 2) {
            return ((b[0] & 0xFF) << 8) + (b[1] & 0xFF);
        } else if (b.length == 3) {
            return ((b[0] & 0xFF) << 16) + ((b[1] & 0xFF) << 8) + (b[2] & 0xFF);
        } else if (b.length == 4) {
            return (b[0] << 24) + ((b[1] & 0xFF) << 16) + ((b[2] & 0xFF) << 8) + (b[3] & 0xFF);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Convert the int to a byte array
     *
     * @param value The integer
     * @return The byte array
     */
    public static byte[] intToByteArray(int value) {
        byte[] result = new byte[4];
        result[3] = (byte) (value & 0xff);
        result[2] = (byte) ((value >> 8) & 0xff);
        result[1] = (byte) ((value >> 16) & 0xff);
        result[0] = (byte) ((value >> 24) & 0xff);
        return result;
    }

    /**
     * Convert the byte array to a short
     *
     * @param b Input byte array
     * @return The short
     */
    public static short byteArrayToShort(byte[] b) {
        if (b.length == 1) {
            return (short) (b[0] & 0xFF);
        } else if (b.length == 2) {
            return (short) (((b[0] & 0xFF) << 8) + (b[1] & 0xFF));
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Convert the short to a byte array
     *
     * @param value The short
     * @return The byte array
     */
    public static byte[] shortToByteArray(short value) {
        byte[] result = new byte[2];
        result[1] = (byte) (value & 0xff);
        result[0] = (byte) ((value >> 8) & 0xff);
        return result;
    }

    /**
     * Convert the byte array to a short
     *
     * @param b The byte array
     * @return The short
     */
    public static byte byteArrayToByte(byte[] b) {
        if (b.length == 1) {
            return (byte) (b[0] & 0xFF);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Convert the short to a byte array
     *
     * @param value The short
     * @return The byte array
     */
    public static byte[] byteToByteArray(byte value) {
        byte[] result = new byte[1];
        result[0] = (byte) (value & 0xff);
        return result;
    }

    /**
     * Compare two byte arrays (0 = Success, 1 = Error)
     *
     * @param array1       Byte Array 1
     * @param array1offset Byte Array 1 offset
     * @param array2       Byte Array 2
     * @param array2offset Byte Array 2 offset
     * @param len          Byte array length
     * @return Comparison result
     */
    public static boolean compareByteArrays(byte[] array1, int array1offset, byte[] array2, int array2offset, int len) {
        int i, j;
        for (i = array1offset, j = array2offset; j < len; i++, j++) {
            if (array1[i] != array2[j]) {
                return false;
            }
        }

        if (((i - array1offset) == len) && ((j - array2offset) == len)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if byte array is all zeros
     *
     * @param bytes Input byte array
     * @return True if all zeros, else false
     */
    public static boolean arrayIsAllZeros(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != 0x00) {
                return false;
            }
        }

        return true;
    }

    /**
     * Extract data from byte array buffer
     *
     * @param buffer Byte array
     * @param length Length to extract
     * @param offset Offset in byte array
     * @return Extracted byte array
     */
    public static byte[] extract(byte[] buffer, int length, int offset) {
        byte[] result = new byte[length];
        System.arraycopy(buffer, offset, result, 0, length);
        return result;
    }

    /**
     * Trim leading zeros from byte array 000001020304 to 01020304
     *
     * @param inputBytes String to trim
     * @return trimmed String
     */
    public static byte[] trimLeadingZeros(byte[] inputBytes) {
        int i = 0;
        for (i = 0; i < inputBytes.length; i++) {
            if (inputBytes[i] != 0x00) {
                break;
            }
        }

        if (i == inputBytes.length) {
            return null;
        } else {
            byte[] outputBytes = new byte[inputBytes.length - i];
            System.arraycopy(inputBytes, i, outputBytes, 0, outputBytes.length);
            return outputBytes;
        }
    }

    /**
     * Concatenates the two given arrays
     *
     * @param b1 the first byte array
     * @param b2 the second byte array
     * @return the resulting byte array
     */
    public static byte[] concat(byte[] b1, byte[] b2) {
        if (b1 == null) {
            return b2;
        } else if (b2 == null) {
            return b1;
        } else {
            byte[] result = new byte[b1.length + b2.length];
            System.arraycopy(b1, 0, result, 0, b1.length);
            System.arraycopy(b2, 0, result, b1.length, b2.length);
            return result;
        }
    }

    /**
     * Concatenates the three given arrays
     *
     * @param b1 the first byte array
     * @param b2 the second byte array
     * @param b3 the third byte array
     * @return the resulting byte array
     */
    public static byte[] concat(byte[] b1, byte[] b2, byte[] b3) {
        if (b1 == null) {
            return concat(b2, b3);
        } else if (b2 == null) {
            return concat(b1, b3);
        } else if (b3 == null) {
            return concat(b1, b2);
        } else {
            byte[] result = new byte[b1.length + b2.length + b3.length];
            System.arraycopy(b1, 0, result, 0, b1.length);
            System.arraycopy(b2, 0, result, b1.length, b2.length);
            System.arraycopy(b3, 0, result, b1.length + b2.length, b3.length);
            return result;
        }
    }

    /**
     * Concatenates the four given arrays
     *
     * @param b1 the first byte array
     * @param b2 the second byte array
     * @param b3 the third byte array
     * @param b4 the fourth byte array
     * @return the resulting byte array
     */
    public static byte[] concat(byte[] b1, byte[] b2, byte[] b3, byte[] b4) {
        if (b1 == null) {
            return concat(b2, b3, b4);
        } else if (b2 == null) {
            return concat(b1, b3, b4);
        } else if (b3 == null) {
            return concat(b1, b2, b4);
        } else if (b4 == null) {
            return concat(b1, b2, b3);
        } else {
            byte[] result = new byte[b1.length + b2.length + b3.length + b4.length];
            System.arraycopy(b1, 0, result, 0, b1.length);
            System.arraycopy(b2, 0, result, b1.length, b2.length);
            System.arraycopy(b3, 0, result, b1.length + b2.length, b3.length);
            System.arraycopy(b4, 0, result, b1.length + b2.length + b3.length, b4.length);
            return result;
        }
    }

    /**
     * Revert byte array
     *
     * @param data Byte array
     * @return Reverted byte array
     */
    public static byte[] revert(byte[] data) {
        int length = data.length;
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = data[length - 1 - i];
        }
        return result;
    }
}
