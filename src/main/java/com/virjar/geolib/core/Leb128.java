package com.virjar.geolib.core;


import java.nio.ByteBuffer;

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
