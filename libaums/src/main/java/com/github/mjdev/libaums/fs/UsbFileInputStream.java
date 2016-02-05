package com.github.mjdev.libaums.fs;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * UsbFileInputStream provides common InputStream access to a UsbFile.
 */
public class UsbFileInputStream extends InputStream {

    private UsbFile file;
    private int currentByteOffset = 0;

    public UsbFileInputStream(UsbFile file) {

        if(file.isDirectory()) {
            throw new RuntimeException("UsbFileInputStream cannot be created on directory!");
        }

        this.file = file;
    }

    @Override
    public int available() throws IOException {
        Log.d("aaaaa", "aaaaaaaaaa");
        return (int) (file.getLength() - currentByteOffset);
    }

    @Override
    public int read() throws IOException {
        if(currentByteOffset >= file.getLength()) {
            return -1;
        }

        ByteBuffer buffer = ByteBuffer.allocate(512);
        file.read(currentByteOffset, buffer);
        currentByteOffset++;
        buffer.flip();
        return buffer.get();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return super.read(buffer);
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        return super.read(buffer, byteOffset, byteCount);
    }

    @Override
    public long skip(long byteCount) throws IOException {
        long skippedBytes = Math.min(byteCount, file.getLength() - currentByteOffset);
        currentByteOffset += skippedBytes;
        return skippedBytes;
    }
}
