package com.github.mjdev.libaums.fs.fat32;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.driver.ByteBlockDevice;
import com.github.mjdev.libaums.driver.file.FileBlockDeviceDriver;
import com.github.mjdev.libaums.util.Pair;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractImpl;
import org.xenei.junit.contract.ContractSuite;
import org.xenei.junit.contract.IProducer;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.fail;

/**
 * Created by magnusja on 02/08/17.
 */
@RunWith(ContractSuite.class)
@ContractImpl(Fat32FileSystem.class)
public class Fat32FileSystemTest {

    IProducer<Pair<Fat32FileSystem, JsonObject>> producer =
            new IProducer<Pair<Fat32FileSystem, JsonObject>>() {

                private BlockDeviceDriver blockDevice;

                public Pair<Fat32FileSystem, JsonObject> newInstance() {
                    try {
                        JsonObject obj = Json.parse(IOUtils.toString(
                                new URL("https://www.dropbox.com/s/nek7bu08prykkhv/expectedValues.json?dl=1")
                                        .openStream())).asObject();

                        if (blockDevice == null) {
                            blockDevice = new ByteBlockDevice(
                                    new FileBlockDeviceDriver(
                                    new URL("https://www.dropbox.com/s/3bxngiqmwitlucd/mbr_fat32.img?dl=1"),
                                    obj.get("blockSize").asInt(),
                                    obj.get("blockSize").asInt() * obj.get("fileSystemOffset").asInt()));
                            blockDevice.init();
                        }
                        return new Pair<>(Fat32FileSystem.read(blockDevice), obj);
                    } catch (IOException e) {
                        e.printStackTrace();
                        fail();
                    }

                    return null;
                }

                public void cleanUp() {
                    // no cleanup required.
                }
            };

    @Contract.Inject
    public IProducer<Pair<Fat32FileSystem, JsonObject>> makeFat32FileSystem() {
        return producer;
    }
}