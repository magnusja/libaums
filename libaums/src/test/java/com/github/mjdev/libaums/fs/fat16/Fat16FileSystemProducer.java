package com.github.mjdev.libaums.fs.fat16;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.driver.ByteBlockDevice;
import com.github.mjdev.libaums.driver.file.FileBlockDeviceDriver;
import com.github.mjdev.libaums.util.Pair;

import org.apache.commons.io.IOUtils;
import org.xenei.junit.contract.IProducer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.fail;

public class Fat16FileSystemProducer implements IProducer<Pair<Fat16FileSystem, JsonObject>> {
    private File originalFile;
    private File tempFile;
    private BlockDeviceDriver blockDevice;
    private JsonObject expectedValues;

    public Fat16FileSystemProducer(String jsonUrl, String imageUrl) {
        try {
            expectedValues = Json.parse(IOUtils.toString(
                    new URL(jsonUrl)
                            .openStream())).asObject();

            URL url = new URL(imageUrl);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            File tempFile = File.createTempFile("libaums_test_blockdevice", ".bin");
            tempFile.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();

            byte[] buffer = new byte[1024];

            File unzipped = File.createTempFile("unzipped-file", ".iso");
            unzipped.deleteOnExit();

            GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(tempFile));
            FileOutputStream unzippedFOS = new FileOutputStream(unzipped);

            int len;
            while ((len = gzis.read(buffer)) > 0) {
                unzippedFOS.write(buffer, 0, len);
            }

            gzis.close();
            unzippedFOS.close();

            this.originalFile = unzipped;

            // force first copy
            cleanUp();

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

    }

    public synchronized Pair<Fat16FileSystem, JsonObject> newInstance() {
        try {
            blockDevice = new ByteBlockDevice(
                    new FileBlockDeviceDriver(
                            tempFile,
                            expectedValues.get("blockSize").asInt(),
                            expectedValues.get("blockSize").asInt() * expectedValues.get("fileSystemOffset").asInt()));
            blockDevice.init();
            return new Pair<>(Fat16FileSystem.read(blockDevice), expectedValues);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        return null;
    }

    public synchronized void cleanUp() {
        try {
            ReadableByteChannel rbc = Channels.newChannel(new FileInputStream(originalFile));
            File tempFile = File.createTempFile("libaums_test_blockdevice", ".bin");
            tempFile.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            this.tempFile = tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
