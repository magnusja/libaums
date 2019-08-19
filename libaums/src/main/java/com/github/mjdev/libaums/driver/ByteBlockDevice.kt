package com.github.mjdev.libaums.driver

import java.io.IOException
import java.nio.ByteBuffer

/**
 * Simple class which wraps around an existing [BlockDeviceDriver] to enable byte addressing
 * of content. Uses byte offsets instead of device oddsets in [ByteBlockDevice.write]
 * and [ByteBlockDevice.read]. Uses [BlockDeviceDriver.getBlockSize]
 * to calculate device offsets.
 */
open class ByteBlockDevice @JvmOverloads constructor(private val targetBlockDevice: BlockDeviceDriver, private val logicalOffsetToAdd: Int = 0) : BlockDeviceDriver {
    override val blockSize: Int
        get() = targetBlockDevice.blockSize

    override val blocks: Long
        get() = targetBlockDevice.blocks
    
    @Throws(IOException::class)
    override fun init() {
        targetBlockDevice.init()
    }

    @Throws(IOException::class)
    override fun read(byteOffset: Long, dest: ByteBuffer) {
        var devOffset = byteOffset / blockSize + logicalOffsetToAdd
        // TODO try to make this more efficient by for example making tmp buffer
        // global
        if (byteOffset % blockSize != 0L) {
            //Log.w(TAG, "device offset " + offset + " not a multiple of block size");
            val tmp = ByteBuffer.allocate(blockSize)

            targetBlockDevice.read(devOffset, tmp)
            tmp.clear()
            tmp.position((byteOffset % blockSize).toInt())
            val limit = Math.min(dest.remaining(), tmp.remaining())
            tmp.limit(tmp.position() + limit)
            dest.put(tmp)

            devOffset++
        }

        if (dest.remaining() > 0) {
            val buffer: ByteBuffer
            if (dest.remaining() % blockSize != 0) {
                //Log.w(TAG, "we have to round up size to next block sector");
                val rounded = blockSize - dest.remaining() % blockSize + dest.remaining()
                buffer = ByteBuffer.allocate(rounded)
                buffer.limit(rounded)
            } else {
                buffer = dest
            }

            targetBlockDevice.read(devOffset, buffer)

            if (dest.remaining() % blockSize != 0) {
                System.arraycopy(buffer.array(), 0, dest.array(), dest.position(), dest.remaining())
            }

            dest.position(dest.limit())
        }
    }

    @Throws(IOException::class)
    override fun write(byteOffset: Long, src: ByteBuffer) {
        var devOffset = byteOffset / blockSize + logicalOffsetToAdd
        // TODO try to make this more efficient by for example making tmp buffer
        // global
        if (byteOffset % blockSize != 0L) {
            //Log.w(TAG, "device offset " + offset + " not a multiple of block size");
            val tmp = ByteBuffer.allocate(blockSize)

            targetBlockDevice.read(devOffset, tmp)
            tmp.clear()
            tmp.position((byteOffset % blockSize).toInt())
            val remaining = Math.min(tmp.remaining(), src.remaining())
            tmp.put(src.array(), src.position(), remaining)
            src.position(src.position() + remaining)
            tmp.clear()
            targetBlockDevice.write(devOffset, tmp)

            devOffset++
        }

        if (src.remaining() > 0) {
            // TODO try to make this more efficient by for example only allocating
            // blockSize and making it global
            val buffer: ByteBuffer
            if (src.remaining() % blockSize != 0) {
                //Log.w(TAG, "we have to round up size to next block sector");
                val rounded = blockSize - src.remaining() % blockSize + src.remaining()
                buffer = ByteBuffer.allocate(rounded)
                buffer.limit(rounded)

                // TODO: instead of just writing 0s at the end of the buffer do we need to read what
                // is currently on the disk and save that then?
                System.arraycopy(src.array(), src.position(), buffer.array(), 0, src.remaining())

                src.position(src.limit())
            } else {
                buffer = src
            }

            targetBlockDevice.write(devOffset, buffer)
        }
    }

    companion object {

        private val TAG = ByteBlockDevice::class.java.simpleName
    }
} 