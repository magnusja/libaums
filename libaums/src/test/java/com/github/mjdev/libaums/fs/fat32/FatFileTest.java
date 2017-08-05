package com.github.mjdev.libaums.fs.fat32;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.driver.ByteBlockDevice;
import com.github.mjdev.libaums.driver.file.FileBlockDeviceDriver;
import com.github.mjdev.libaums.util.Pair;

import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractImpl;
import org.xenei.junit.contract.ContractSuite;
import org.xenei.junit.contract.IProducer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.junit.Assert.*;

/**
 * Created by magnusja on 03/08/17.
 */
@RunWith(ContractSuite.class)
@ContractImpl(FatFile.class)
public class FatFileTest {

    private static class Producer implements IProducer<Pair<Fat32FileSystem, JsonObject>> {
        private File originalFile;
        private File tempFile;
        private BlockDeviceDriver blockDevice;
        private JsonObject expecteValues;

        public Producer() {
            try {
                expecteValues = Json.parse(IOUtils.toString(
                        new URL("https://www.dropbox.com/s/nek7bu08prykkhv/expectedValues.json?dl=1")
                                .openStream())).asObject();

                URL url = new URL("https://www.dropbox.com/s/3bxngiqmwitlucd/mbr_fat32.img?dl=1");
                ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                File tempFile = File.createTempFile("blockdevice", ".bin");
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
                File tempFile = File.createTempFile("tmp_blockdevice", ".bin");
                FileOutputStream fos = new FileOutputStream(tempFile);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                this.tempFile = tempFile;
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }
        }
    }

    private Producer producer = new Producer();

    @Contract.Inject
    public IProducer<Pair<Fat32FileSystem, JsonObject>> makeFat32FileSystem() {
        return producer;
    }

}