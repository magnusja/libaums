package com.github.mjdev.libaums.fs;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * UsbFileOutputStream provides common OutputStream access to a UsbFile.
 */
public class UsbFileOutputStream extends OutputStream {

    private UsbFile file;
    private int currentByteOffset = 0;

    public UsbFileOutputStream(@NonNull UsbFile file) {

        if(file.isDirectory()) {
            throw new RuntimeException("UsbFileOutputStream cannot be created on directory!");
        }

        this.file = file;
    }

    @Override
    public void write(int oneByte) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[] {(byte) oneByte});
        file.write(currentByteOffset, byteBuffer);

        currentByteOffset++;
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    @Override
    public void flush() throws IOException {
        file.flush();
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        file.write(currentByteOffset, byteBuffer);

        currentByteOffset += buffer.length;
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

        byteBuffer.position(offset);
        byteBuffer.limit(count + offset);

        file.write(currentByteOffset, byteBuffer);

        currentByteOffset += count;
    }
}
