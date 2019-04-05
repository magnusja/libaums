package com.github.mjdev.libaums.fs.fat16;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnsignedUtil {

    public static int getUnsignedInt16(int offset, ByteBuffer data) {
        final int i1 = data.get(offset) & 0xff;
        final int i2 = data.get(offset + 1) & 0xff;
        return (i2 << 8) | i1;
    }

    public static void setUnsignedInt16(int offset, int value, ByteBuffer data) {
        data.put(offset, (byte) (value & 0xff));
        data.put(offset + 1, (byte) ((value >>> 8) & 0xff));
    }

    public static ByteBuffer allocateLittleEndian(int size){
        ByteBuffer allocate = ByteBuffer.allocate(size);
        allocate.order(ByteOrder.LITTLE_ENDIAN);
        return allocate;
    }

}
