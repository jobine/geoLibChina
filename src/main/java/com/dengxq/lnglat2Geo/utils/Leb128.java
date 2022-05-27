package com.dengxq.lnglat2Geo.utils;


import java.nio.ByteBuffer;

/**
 * Reads and writes DWARFv3 LEB 128 signed and unsigned integers. See DWARF v3
 * section 7.6.
 * <p>
 * Leb128编码方式能够显著提升数字类型的空间大小，他的核心原理是我们程序运行的数字大多数在一个很小的范围（大部分在千以内）
 * 故小数字短编码，大数字长编码。可以使用一个byte存储long类型的数字
 * <p>
 * Leb128带来的问题也是数据区域更加不可读，但是这是我们优化存储空间的有效手段
 * <p>
 * <p>
 * 同样是否需要使用Leb128也是参考我们对数据的范围本身的大小参考
 */
public final class Leb128 {
    private Leb128() {
    }

    /**
     * Gets the number of bytes in the unsigned LEB128 encoding of the
     * given value.
     *
     * @param value the value in question
     * @return its write size, in bytes
     */
    public static int unsignedLeb128Size(int value) {
        // TODO: This could be much cleverer.

        int remaining = value >> 7;
        int count = 0;

        while (remaining != 0) {
            remaining >>= 7;
            count++;
        }

        return count + 1;
    }

    /**
     * Reads an signed integer from {@code in}.
     */
    public static int readSignedLeb128(ByteBuffer in) {
        int result = 0;
        int cur;
        int count = 0;
        int signBits = -1;

        do {
            cur = in.get() & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            signBits <<= 7;
            count++;
        } while (((cur & 0x80) == 0x80) && count < 5);

        if ((cur & 0x80) == 0x80) {
            throw new RuntimeException("invalid LEB128 sequence");
        }

        // Sign extend if appropriate
        if (((signBits >> 1) & result) != 0) {
            result |= signBits;
        }

        return result;
    }

    /**
     * Reads an unsigned integer from {@code in}.
     */
    public static int readUnsignedLeb128(ByteBuffer in) {
        int result = 0;
        int cur;
        int count = 0;

        do {
            cur = in.get() & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            count++;
        } while (((cur & 0x80) == 0x80) && count < 5);

        if ((cur & 0x80) == 0x80) {
            throw new RuntimeException("invalid LEB128 sequence");
        }

        return result;
    }

    /**
     * Writes {@code value} as an unsigned integer to {@code out}, starting at
     * {@code offset}. Returns the number of bytes written.
     */
    public static void writeUnsignedLeb128(ByteBuffer out, int value) {
        int remaining = value >>> 7;

        while (remaining != 0) {
            out.put((byte) ((value & 0x7f) | 0x80));
            value = remaining;
            remaining >>>= 7;
        }

        out.put((byte) (value & 0x7f));
    }

    /**
     * Writes {@code value} as a signed integer to {@code out}, starting at
     * {@code offset}. Returns the number of bytes written.
     */
    public static void writeSignedLeb128(ByteBuffer out, int value) {
        int remaining = value >> 7;
        boolean hasMore = true;
        int end = ((value & Integer.MIN_VALUE) == 0) ? 0 : -1;

        while (hasMore) {
            hasMore = (remaining != end)
                    || ((remaining & 1) != ((value >> 6) & 1));

            out.put((byte) ((value & 0x7f) | (hasMore ? 0x80 : 0)));
            value = remaining;
            remaining >>= 7;
        }
    }
}
