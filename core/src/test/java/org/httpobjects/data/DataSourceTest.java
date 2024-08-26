package org.httpobjects.data;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

abstract class DataSourceTest {

    abstract DataSource createTestSubject(byte[] data);

    @Test
    public void readInputStreamAsync(){
        // given
        final byte[] data = "hello world".getBytes();
        final DataSource subject = createTestSubject(data);
        final byte[] buffer = new byte[1024];

        // when
        final byte[] output = subject
                .readInputStreamAsync()
                .map(channel -> DataSetUtil.readAllBytes(channel, buffer))
                .join();

        // then
        Assert.assertArrayEquals(data, output);
    }

    @Test
    public void readChannelAsync(){
        // given
        final byte[] data = "hello world".getBytes();
        final DataSource subject = createTestSubject(data);
        final ByteBuffer buffer = ByteBuffer.allocate(100);

        // when
        final byte[] output = subject
                .readChannelAsync()
                .map(channel -> DataSetUtil.readAllBytes(channel, buffer))
                .join();

        // then
        Assert.assertArrayEquals(data, output);
    }


    @Test
    public void writeSync(){
        // given
        final byte[] data = "hello world".getBytes();
        final DataSource subject = createTestSubject(data);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        // when
        subject.writeSync(out);

        // then
        final byte[] output = out.toByteArray();
        Assert.assertArrayEquals(data, output);
    }



    @Test
    public void decodeToUTF8WithLimits(){
        // given
        final byte[] data = "hello world".getBytes();
        final DataSource subject = createTestSubject(data);

        // when
        final String result = subject.decodeToUTF8(5);

        // then

        final String expected = subject.enforcesLimits() ? "hello" : "hello world";
        Assert.assertEquals(expected, result);
    }


    @Test
    public void decodeToUTF8Unbounded(){
        // given
        final byte[] data = "hello world".getBytes();
        final DataSource subject = createTestSubject(data);

        // when
        final String result = subject.decodeToUTF8Unbounded();

        // then
        Assert.assertEquals("hello world", result);
    }


    @Test
    public void decodeToAsciiUnbounded(){
        // given
        final byte[] data = "hello world".getBytes();
        final DataSource subject = createTestSubject(data);

        // when
        final String result = subject.decodeToAsciiUnbounded();

        // then
        Assert.assertEquals("hello world", result);
    }


    @Test
    public void decodeToAsciiWithLimits(){
        // given
        final byte[] data = "hello world".getBytes();
        final DataSource subject = createTestSubject(data);

        // when
        final String result = subject.decodeToAscii(5);

        // then

        final String expected = subject.enforcesLimits() ? "hello" : "hello world";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void readToMemoryWithLimits(){
        // given
        final byte[] data = "hello world".getBytes();
        final DataSource subject = createTestSubject(data);

        // when
        final byte[] result = subject.readToMemory(5);

        // then

        final String expectedString = subject.enforcesLimits() ? "hello" : "hello world";
        Assert.assertEquals(expectedString, new String(result));

        final byte[] expectedData = subject.enforcesLimits() ? "hello".getBytes() : data;
        Assert.assertArrayEquals(expectedData, result);
    }
}
