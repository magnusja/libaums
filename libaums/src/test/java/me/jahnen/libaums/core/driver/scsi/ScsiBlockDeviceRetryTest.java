package me.jahnen.libaums.core.driver.scsi;

import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import me.jahnen.libaums.core.usb.UsbCommunication;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ScsiBlockDeviceRetryTest {

    /**
     * Minimal bulk-only transport target. Fails the CSW read of the first WRITE(10) once, after
     * the whole data-out phase went through, which makes the driver retry the command.
     */
    private static class FlakyDevice implements UsbCommunication {
        private int tag, dataLength, opcode;
        private boolean expectCbw = true, statusPhase;
        private boolean failWriteStatus = true;
        ByteBuffer lastWrite;

        @Override
        public int bulkOutTransfer(ByteBuffer src) {
            int length = src.remaining();
            if (expectCbw) {
                ByteBuffer cbw = src.duplicate().order(ByteOrder.LITTLE_ENDIAN);
                tag = cbw.getInt(src.position() + 4);
                dataLength = cbw.getInt(src.position() + 8);
                opcode = cbw.get(src.position() + 15) & 0xFF;
                expectCbw = false;
                statusPhase = dataLength == 0;
            } else {
                lastWrite = ByteBuffer.allocate(length);
                lastWrite.put(src.array(), src.position(), length);
                statusPhase = true;
            }
            src.position(src.position() + length);
            return length;
        }

        @Override
        public int bulkInTransfer(ByteBuffer dest) throws IOException {
            if (!statusPhase) {
                byte[] data = new byte[dataLength];
                if (opcode == 0x25) { // READ CAPACITY(10)
                    ByteBuffer.wrap(data).putInt(2047).putInt(512);
                }
                dest.put(data);
                statusPhase = true;
                return data.length;
            }
            expectCbw = true;
            statusPhase = false;
            if (opcode == 0x2A && failWriteStatus) {
                failWriteStatus = false;
                throw new IOException("CSW read timed out");
            }
            dest.order(ByteOrder.LITTLE_ENDIAN).putInt(0x53425355).putInt(tag).putInt(0).put((byte) 0);
            return 13;
        }

        @Override
        public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length) {
            return 0;
        }

        @Override public void resetDevice() {}
        @Override public void clearFeatureHalt(UsbEndpoint endpoint) {}
        @Override public void close() {}
        @Override public UsbEndpoint getInEndpoint() { return null; }
        @Override public UsbEndpoint getOutEndpoint() { return null; }
        @Override public UsbInterface getUsbInterface() { return null; }
    }

    @Test
    public void retriedWriteResendsWholeBuffer() throws IOException {
        FlakyDevice device = new FlakyDevice();
        ScsiBlockDevice blockDevice = new ScsiBlockDevice(device, (byte) 0);
        blockDevice.init();

        byte[] data = new byte[4 * 512];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // Used to throw IllegalArgumentException: newLimit > capacity: (4096 > 2048)
        blockDevice.write(0, buffer);

        assertEquals(data.length, buffer.position());
        assertArrayEquals(data, device.lastWrite.array());
    }
}
