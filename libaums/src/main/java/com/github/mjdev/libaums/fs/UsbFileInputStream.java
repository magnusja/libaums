/*
 * (C) Copyright 2016 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
        // return 0, because we always block
        return 0;
    }

    @Override
    public int read() throws IOException {

        if(currentByteOffset >= file.getLength()) {
            return -1;
        }

        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.limit(1);
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
