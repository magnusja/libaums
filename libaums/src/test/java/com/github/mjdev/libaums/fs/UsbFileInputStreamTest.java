package com.github.mjdev.libaums.fs;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by magnusja on 12/08/17.
 */
public class UsbFileInputStreamTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Captor
    private ArgumentCaptor<Long> longCaptor;
    @Captor
    private ArgumentCaptor<ByteBuffer> byteBufferCaptor;

    @Mock
    private UsbFile file;

    @Before
    public void setUp() throws Exception {
        when(file.getLength()).thenReturn((long) 123);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void throwIfDirectory() {
        when(file.isDirectory()).thenReturn(true);

        new UsbFileInputStream(file);
    }

    @Test
    public void close() throws Exception {
        new UsbFileInputStream(file).close();

        verify(file).close();
    }

    @Test
    public void readInt() throws Exception {

        InputStream is = new UsbFileInputStream(file);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer buffer = invocation.getArgument(1);
                buffer.put((byte) 1);
                return null;
            }
        }).when(file).read(any(Long.class), any(ByteBuffer.class));

        for (int i = 0; i < 20; i++) {
            int b = is.read();
            assertEquals(1, b);
            verify(file, times(i + 1)).read(longCaptor.capture(), byteBufferCaptor.capture());
            assertEquals(i, longCaptor.getValue().longValue());
            assertNotNull(byteBufferCaptor.getValue());
        }
    }

    @Test
    public void read() throws Exception {
        InputStream is = new UsbFileInputStream(file);

        is.read(new byte[50]);
        verify(file).read(longCaptor.capture(), byteBufferCaptor.capture());
        assertEquals(0, longCaptor.getValue().longValue());
        assertEquals(50, byteBufferCaptor.getValue().remaining());

        is.read(new byte[50]);
        verify(file, times(2)).read(longCaptor.capture(), byteBufferCaptor.capture());
        assertEquals(50, longCaptor.getValue().longValue());
        assertEquals(50, byteBufferCaptor.getValue().remaining());
    }

    @Test
    public void readOffset() throws Exception {
        InputStream is = new UsbFileInputStream(file);

        is.read(new byte[50], 0, 50);
        verify(file).read(longCaptor.capture(), byteBufferCaptor.capture());
        assertEquals(0, longCaptor.getValue().longValue());
        assertEquals(50, byteBufferCaptor.getValue().remaining());

        is.read(new byte[50], 10, 30);
        verify(file, times(2)).read(longCaptor.capture(), byteBufferCaptor.capture());
        assertEquals(50, longCaptor.getValue().longValue());
        assertEquals(10, byteBufferCaptor.getValue().position());
        assertEquals(30, byteBufferCaptor.getValue().remaining());
    }

    @Test
    public void skip() throws Exception {
        InputStream is = new UsbFileInputStream(file);

        long skipped = is.skip(50);
        assertEquals(50, skipped);

        is.read(new byte[1]);
        verify(file).read(longCaptor.capture(), byteBufferCaptor.capture());
        assertEquals(50, longCaptor.getValue().longValue());

        skipped = is.skip(100);
        assertEquals(123 - 50 - 1, skipped);
    }

    @Test
    public void readMoreThenLength() throws Exception {
        InputStream is = new UsbFileInputStream(file);

        int read = is.read(new byte[200]);
        assertEquals(123, read);

        read = is.read(new byte[200]);
        assertEquals(-1, read);
    }

}