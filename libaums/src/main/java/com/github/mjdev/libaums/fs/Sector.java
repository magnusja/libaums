package com.github.mjdev.libaums.fs;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Yuriy on 25.09.2016.
 */

public class Sector {

    /**
     * The buffer holding the contents of this {@code Sector}.
     */
    protected final ByteBuffer buffer;

    private boolean dirty;

    protected Sector(ByteBuffer byteBuffer) {
        this.buffer = byteBuffer;
        this.dirty = true;
    }


    public final boolean isDirty() {
        return this.dirty;
    }

    protected final void markDirty() {
        this.dirty = true;
    }


    protected int get16(int offset) {
        return buffer.getShort(offset) & 0xffff;
    }

    protected long get32(int offset) {
        return buffer.getInt(offset);
    }

    protected int get8(int offset) {
        return buffer.get(offset) & 0xff;
    }

    protected void set16(int offset, int value) {
        buffer.putShort(offset, (short) (value & 0xffff));
        dirty = true;
    }

    protected void set32(int offset, long value) {
        buffer.putInt(offset, (int) (value & 0xffffffff));
        dirty = true;
    }

    protected void set8(int offset, int value) {
        if ((value & 0xff) != value) {
            throw new IllegalArgumentException(
                    value + " too big to be stored in a single octet");
        }

        buffer.put(offset, (byte) (value & 0xff));
        dirty = true;
    }
}
