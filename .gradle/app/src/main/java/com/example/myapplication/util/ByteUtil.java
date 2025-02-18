package com.example.myapplication.util;


public final class ByteUtil {


    /**
     * Byte转Bit
     */
    public static String byte2BitStr(byte b) {
        return "" + (byte) ((b >> 7) & 0x1) +
                (byte) ((b >> 6) & 0x1) +
                (byte) ((b >> 5) & 0x1) +
                (byte) ((b >> 4) & 0x1) +
                (byte) ((b >> 3) & 0x1) +
                (byte) ((b >> 2) & 0x1) +
                (byte) ((b >> 1) & 0x1) +
                (byte) ((b >> 0) & 0x1);
    }

    public static String byte2ReserveBitStr(byte[] arr) {
        StringBuilder sb = new StringBuilder("");
        for (byte b : arr) {
            sb.append(byte2BitStr(b));
        }
        return sb.reverse().toString();
    }

    public static String byte2BitStr(byte[] arr) {
        StringBuilder sb = new StringBuilder("");
        for (byte b : arr) {
            sb.append(byte2BitStr(b));
        }
        return sb.toString();
    }

    /**
     * Bit转Byte
     */
    public static byte bitStr2Byte(String byteStr) {
        int re, len;
        if (null == byteStr) {
            return 0;
        }
        len = byteStr.length();
        if (len != 4 && len != 8) {
            return 0;
        }
        if (len == 8) {// 8 bit处理
            if (byteStr.charAt(0) == '0') {// 正数
                re = Integer.parseInt(byteStr, 2);
            } else {// 负数
                re = Integer.parseInt(byteStr, 2) - 256;
            }
        } else {//4 bit处理
            re = Integer.parseInt(byteStr, 2);
        }
        return (byte) re;
    }


    /**
     * 将表示16进制值的字符串转换为byte数组，和public static String byteArrToHexStr(byte[] arrB)
     * 互为可逆的转换过程
     *
     * @param hex 需要转换的字符串
     * @return 转换后的byte数组
     */
    public static byte[] hexStrToByteArr(String hex) {
        int len = hex.length();
        if (len % 2 == 1) {
            hex = "0" + hex;
            len++;
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] byteAdd(byte[] a, byte[] b) {
        byte[] ret = new byte[a.length + b.length];
        for (int i = 0; i < a.length; i++) {
            ret[i] = a[i];
        }
        for (int j = 0; j < b.length; j++) {
            ret[a.length + j] = b[j];
        }
        return ret;

    }

    public static byte[] byteSpace(byte[] a, int len) {
        String b = "";
        for (int k = 0; k < len - a.length; k++) {
            b += " ";
        }
        byte[] ret = new byte[a.length + b.getBytes().length];
        for (int i = 0; i < a.length; i++) {
            ret[i] = a[i];
        }
        for (int j = 0; j < b.getBytes().length; j++) {
            ret[a.length + j] = b.getBytes()[j];
        }
        return ret;
    }

    public static byte[] subByte(byte[] a, int start, int end) {
        byte[] ret = new byte[end - start];
        for (int i = 0; i < end - start; i++) {
            ret[i] = a[start + i];
        }
        return ret;
    }

    public static byte[] rtrimBytes(byte[] src) {

        int i = src.length - 1;
        for (; i >= 0; i--) {
            if (src[i] != 0) {
                break;
            }
        }
        if (i == src.length - 1) {
            return src;
        }
        if (i == -1) {
            return new byte[0];
        }
        byte[] tmp = new byte[i + 1];
        System.arraycopy(src, 0, tmp, 0, i + 1);
        return tmp;
    }

    public static byte[] ltrimBytes(byte[] src) {

        int i = 0;
        for (; i < src.length; i++) {
            if (src[i] != 0) {
                break;
            }
        }
        if (i == src.length) {
            return new byte[0];
        }
        ;

        if (i == 0) {
            return src;
        }
        ;

        byte[] tmp = new byte[src.length - i];
        System.arraycopy(src, i, tmp, 0, src.length - i);
        return tmp;
    }

    /**
     * void
     * 2019年10月24日
     */
    public static void rfillBytes(byte[] src, int len, byte[] bytes, int offset) {
        if (src.length == len) {
            System.arraycopy(src, 0, bytes, offset, len);
        } else if (src.length > len) {
            System.arraycopy(src, 0, bytes, offset, len);
        } else {
            System.arraycopy(src, 0, bytes, offset, src.length);
            for (int i = offset + src.length; i < offset + len; i++) {
                bytes[i] = 0;
            }
        }
    }


    /**
     * 8 bytes to long
     *
     * @param abyte0
     * @return
     */
    public static long Bytes8ToLong(byte abyte0[]) {
        long ret = 0;

        ret = (0xffL & abyte0[0]) << 56 | (0xffL & abyte0[1]) << 48 | (0xffL & abyte0[2]) << 40
                | (0xffL & abyte0[3]) << 32 | (0xffL & abyte0[4]) << 24 | (0xffL & abyte0[5]) << 16
                | (0xffL & abyte0[6]) << 8 | 0xffL & abyte0[7];

        return ret;
    }

    /**
     * Long占8个字节
     *
     * @param i
     * @return
     */
    public static byte[] longToBytes8(long i) {
        byte abyte0[] = new byte[8];
        abyte0[7] = (byte) (0xffL & i);
        abyte0[6] = (byte) ((0xff00L & i) >> 8);
        abyte0[5] = (byte) ((0xff0000L & i) >> 16);
        abyte0[4] = (byte) ((0xff000000L & i) >> 24);
        abyte0[3] = (byte) ((0xff00000000L & i) >> 32);
        abyte0[2] = (byte) ((0xff0000000000L & i) >> 40);
        abyte0[1] = (byte) ((0xff000000000000L & i) >> 48);
        abyte0[0] = (byte) ((0xff00000000000000L & i) >> 56);
        return abyte0;
    }

//    public static short byte2ToShort(byte[] b) {
//        return (short) (((b[1] << 8) | b[0] & 0xff));
//    }

    public static short byte2short(byte b[], int offset) {
        return (short) ((b[offset + 1] & 0xff) | (b[offset + 0] & 0xff) << 8);
    }

    public static short byte2short(byte b[]) {
        return (short) ((b[1] & 0xff) | (b[0] & 0xff) << 8);
    }

    public static int byte2int(byte b[], int offset) {
        return b[offset + 3] & 0xff | (b[offset + 2] & 0xff) << 8 | (b[offset + 1] & 0xff) << 16
                | (b[offset] & 0xff) << 24;
    }

    public static int byte2int(byte b[]) {
        return b[3] & 0xff | (b[2] & 0xff) << 8 | (b[1] & 0xff) << 16 | (b[0] & 0xff) << 24;
    }

    public static long byte2long(byte b[]) {
        return (long) b[7] & (long) 255 | ((long) b[6] & (long) 255) << 8 | ((long) b[5] & (long) 255) << 16
                | ((long) b[4] & (long) 255) << 24 | ((long) b[3] & (long) 255) << 32 | ((long) b[2] & (long) 255) << 40
                | ((long) b[1] & (long) 255) << 48 | (long) b[0] << 56;
    }

    public static long byte2long(byte b[], int offset) {
        return (long) b[offset + 7] & (long) 255 | ((long) b[offset + 6] & (long) 255) << 8
                | ((long) b[offset + 5] & (long) 255) << 16 | ((long) b[offset + 4] & (long) 255) << 24
                | ((long) b[offset + 3] & (long) 255) << 32 | ((long) b[offset + 2] & (long) 255) << 40
                | ((long) b[offset + 1] & (long) 255) << 48 | (long) b[offset] << 56;
    }

    public static byte[] int2byte(int n) {
        byte b[] = new byte[4];
        b[0] = (byte) (n >> 24);
        b[1] = (byte) (n >> 16);
        b[2] = (byte) (n >> 8);
        b[3] = (byte) n;
        return b;
    }

    public static void int2byte(int n, byte buf[], int offset) {
        buf[offset] = (byte) (n >> 24);
        buf[offset + 1] = (byte) (n >> 16);
        buf[offset + 2] = (byte) (n >> 8);
        buf[offset + 3] = (byte) n;
    }

    public static byte[] short2byte(int n) {
        byte b[] = new byte[2];
        b[0] = (byte) (n >> 8);
        b[1] = (byte) n;
        return b;
    }

    public static void short2byte(int n, byte buf[], int offset) {
        buf[offset] = (byte) (n >> 8);
        buf[offset + 1] = (byte) n;
    }


    public static byte[] ensureLength(byte[] array, int minLength, int padding) throws Exception {
        if (minLength < 0 || padding < 0)
            throw new Exception();
        if (array.length == minLength)
            return array;
        return array.length > minLength ? copyOf(array, minLength) : copyOf(array, minLength + padding);
    }

    private static byte[] copyOf(byte[] original, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, length));
        return copy;
    }

    public static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }

    /**
     * 给数组填充固定的长度数据
     *
     * @param b
     * @param maxLen
     * @return
     */
    public static byte[] fillZeroLeft(byte[] b, int maxLen) {
        int current = maxLen - b.length;
        if (b.length >= maxLen) return b;
        byte[] newArr = new byte[current];
        return concat(newArr, b);
    }

    /**
     * 给数组填充固定的长度数据
     *
     * @param b
     * @param maxLen
     * @return
     */
    public static byte[] fillZeroRight(byte[] b, int maxLen) {
        int current = maxLen - b.length;
        if (b.length >= maxLen) return b;
        byte[] newArr = new byte[current];
        return concat(b, newArr);
    }

    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();


    public static String bytes2HexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int high = (b & 0xF0) >>> 4;
            int low = b & 0x0F;
            sb.append(HEX_CHARS[high]);
            sb.append(HEX_CHARS[low]);
        }
        return sb.toString();
    }


    public static byte[] reverse(byte[] array) {
        int left = 0;
        int right = array.length - 1;

        while (left < right) {
            byte temp = array[left];
            array[left] = array[right];
            array[right] = temp;

            left++;
            right--;
        }
        return array;
    }



}
