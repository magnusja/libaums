package com.github.mjdev.libaums.driver.file

import com.github.mjdev.libaums.driver.BlockDeviceDriver
import java.io.*
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.Channels

/**
 * Created by magnusja on 01/08/17.
 */

class FileBlockDeviceDriver : BlockDeviceDriver {
    private var file: RandomAccessFile
    override var blockSize: Int = 0
        private set
    private var byteOffset: Int = 0

    override val blocks: Long
        get() = file.length() / blockSize

    @Throws(FileNotFoundException::class)
    @JvmOverloads
    constructor(file: File, byteOffset: Int = 0, blockSize: Int = 512) {
        this.file = RandomAccessFile(file, "rw")
        this.blockSize = blockSize
        this.byteOffset = byteOffset
    }

    @Throws(IOException::class)
    @JvmOverloads
    constructor(url: URL, byteOffset: Int = 0, blockSize: Int = 512) {
        this.byteOffset = byteOffset
        val rbc = Channels.newChannel(url.openStream())
        val tempFile = File.createTempFile("libaums_file_blockdevice", ".bin")
        tempFile.deleteOnExit()
        val fos = FileOutputStream(tempFile)
        fos.channel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)

        this.file = RandomAccessFile(tempFile, "rw")
        this.blockSize = blockSize
    }

    @Throws(IOException::class)
    override fun init() {

    }

    @Throws(IOException::class)
    override fun read(deviceOffset: Long, buffer: ByteBuffer) {
        file.seek(deviceOffset * blockSize + byteOffset)
        val read = file.read(buffer.array(), buffer.position(), buffer.remaining())

        if (read == -1) {
            throw IOException("EOF")
        }

        buffer.position(buffer.position() + read)
    }

    @Throws(IOException::class)
    override fun write(deviceOffset: Long, buffer: ByteBuffer) {
        file.seek(deviceOffset * blockSize + byteOffset)
        file.write(buffer.array(), buffer.position(), buffer.remaining())
        buffer.position(buffer.limit())
    }
}
