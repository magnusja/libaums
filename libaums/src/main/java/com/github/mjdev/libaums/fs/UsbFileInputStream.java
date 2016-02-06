package com.github.mjdev.libaums.fs;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * UsbFileInputStream provides common InputStream access to a UsbFile.
 */
public class UsbFileInputStream extends InputStream {

    private static final String TAG = UsbFileInputStream.class.getSimpleName();
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
        Log.d(TAG, "available");
        return (int) (file.getLength() - currentByteOffset);
    }

    @Override
    public int read() throws IOException {
        Log.d(TAG, "read one byte");

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
        Log.d(TAG, "read into byte array");

        if(currentByteOffset >= file.getLength()) {
            return -1;
        }

        long length = file.getLength();
        long toRead = Math.min(buffer.length, length - currentByteOffset);

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.limit((int) toRead);

        file.read(currentByteOffset, byteBuffer);
        currentByteOffset += toRead;

        return (int) toRead;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        Log.d(TAG, "read into byte array with offset and count");

        if(currentByteOffset >= file.getLength()) {
            return -1;
        }

        long length = file.getLength();
        long toRead = Math.min(byteCount, length - currentByteOffset);

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.position(byteOffset);
        byteBuffer.limit((int) toRead + byteOffset);

        file.read(currentByteOffset, byteBuffer);
        currentByteOffset += toRead;

        return (int) toRead;
    }

    @Override
    public long skip(long byteCount) throws IOException {
        long skippedBytes = Math.min(byteCount, file.getLength() - currentByteOffset);
        currentByteOffset += skippedBytes;
        return skippedBytes;
    }
}
