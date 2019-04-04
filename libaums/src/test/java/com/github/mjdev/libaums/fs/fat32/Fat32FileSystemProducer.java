package com.github.mjdev.libaums.fs.fat32;

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

import static org.junit.Assert.fail;

/**
 * Created by magnusja on 19/09/17.
 */

public class Fat32FileSystemProducer implements IProducer<Pair<Fat32FileSystem, JsonObject>> {
    private File originalFile;
    private File tempFile;
    private BlockDeviceDriver blockDevice;
    private JsonObject expecteValues;

    public Fat32FileSystemProducer(String jsonUrl, String imageUrl) {
        try {
            expecteValues = Json.parse(IOUtils.toString(
                    new URL(jsonUrl)
                            .openStream())).asObject();

            URL url = new URL(imageUrl);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            File tempFile = File.createTempFile("libaums_test_blockdevice", ".bin");
            tempFile.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            this.originalFile = tempFile;

            // force first copy
            cleanUp();

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

    }

    public synchronized Pair<Fat32FileSystem, JsonObject> newInstance() {
        try {
            blockDevice = new ByteBlockDevice(
                    new FileBlockDeviceDriver(
                            tempFile,
                            expecteValues.get("blockSize").asInt(),
                            expecteValues.get("blockSize").asInt() * expecteValues.get("fileSystemOffset").asInt()));
            blockDevice.init();
            return new Pair<>(Fat32FileSystem.read(blockDevice), expecteValues);
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
