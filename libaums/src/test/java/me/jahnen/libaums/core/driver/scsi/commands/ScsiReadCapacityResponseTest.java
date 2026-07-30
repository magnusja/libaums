package me.jahnen.libaums.core.driver.scsi.commands;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ScsiReadCapacityResponseTest {

    @Test
    public void blockCountTreatsLastBlockAddressAsUnsigned() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(0xEE7752AF)
                .putInt(512);
        buffer.flip();

        ScsiReadCapacityResponse response = ScsiReadCapacityResponse.Companion.read(buffer);

        assertEquals(4_000_797_360L, response.getBlockCount());
    }

    @Test
    public void blockCountRejectsReadCapacity16Sentinel() {
        ByteBuffer buffer = ByteBuffer.allocate(8)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(0xFFFFFFFF)
                .putInt(512);
        buffer.flip();

        ScsiReadCapacityResponse response = ScsiReadCapacityResponse.Companion.read(buffer);

        try {
            response.getBlockCount();
            fail("Expected IOException");
        } catch (IOException exception) {
            assertEquals("Device requires SCSI READ CAPACITY(16)", exception.getMessage());
        }
    }
}
