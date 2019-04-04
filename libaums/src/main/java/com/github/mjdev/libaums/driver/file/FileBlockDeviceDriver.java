package com.github.mjdev.libaums.driver.file;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Created by magnusja on 01/08/17.
 */

public class FileBlockDeviceDriver implements BlockDeviceDriver {
    private RandomAccessFile file;
    private int blockSize;
    private int byteOffset;

    public FileBlockDeviceDriver(File file, int blockSize, int byteOffset) throws FileNotFoundException {
        this.file = new RandomAccessFile(file, "rw");
        this.blockSize = blockSize;
        this.byteOffset = byteOffset;
    }

    public FileBlockDeviceDriver(File file, int byteOffset) throws FileNotFoundException {
        this(file, 512, byteOffset);
    }

    public FileBlockDeviceDriver(File file) throws FileNotFoundException {
        this(file, 512, 0);
    }

    public FileBlockDeviceDriver(URL url, int blockSize, int byteOffset) throws IOException {
        this.byteOffset = byteOffset;
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        File tempFile = File.createTempFile("libaums_file_blockdevice", ".bin");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

        this.file = new RandomAccessFile(tempFile, "rw");
        this.blockSize = blockSize;
    }

    public FileBlockDeviceDriver(URL url, int byteOffset) throws IOException {
        this(url, 512, byteOffset);
    }

    public FileBlockDeviceDriver(URL url) throws IOException {
        this(url, 512, 0);
    }

    @Override
    public void init() throws IOException {

    }

    @Override
    public void read(long deviceOffset, ByteBuffer buffer) throws IOException {
        file.seek(deviceOffset * blockSize + byteOffset);
        int read = file.read(buffer.array(), buffer.position(), buffer.remaining());
        buffer.position(buffer.position() + read);
    }

    @Override
    public void write(long deviceOffset, ByteBuffer buffer) throws IOException {
        file.seek(deviceOffset * blockSize + byteOffset);
        file.write(buffer.array(), buffer.position(), buffer.remaining());
        buffer.position(buffer.limit());
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }
}
