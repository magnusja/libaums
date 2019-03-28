package com.github.mjdev.libaums.fs;

import java.io.IOException;
import java.io.InputStream;

public class RepeatedSequenceInputStream extends InputStream {
    private final byte[] sequence;
    private final long size;

    private long position = 0;


    public RepeatedSequenceInputStream(byte[] sequence, long size) {
        this.sequence = sequence;
        this.size = size;
    }

    @Override
    public int read() throws IOException {
        if (position > size)
            return -1;

        byte result = sequence[(int) (position % sequence.length)];
        position++;
        return result;
    }
}
