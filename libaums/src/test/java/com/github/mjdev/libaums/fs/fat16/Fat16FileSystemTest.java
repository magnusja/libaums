package com.github.mjdev.libaums.fs.fat16;

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
@ContractImpl(Fat16FileSystem.class)
public class Fat16FileSystemTest {

    IProducer<Pair<Fat16FileSystem, JsonObject>> producer =
            new IProducer<Pair<Fat16FileSystem, JsonObject>>() {

                private BlockDeviceDriver blockDevice;

                public Pair<Fat16FileSystem, JsonObject> newInstance() {
                    try {
                        JsonObject obj = Json.parse(IOUtils.toString(
                                new URL("https://raw.githubusercontent.com/gopai/iso-store/master/expectedValues.json")
                                        .openStream())).asObject();

                        if (blockDevice == null) {
                            blockDevice = new ByteBlockDevice(
                                    new FileBlockDeviceDriver(
                                            new URL("https://github.com/gopai/iso-store/raw/master/fat16test.iso.gz"),
                                            obj.get("blockSize").asInt(),
                                            obj.get("blockSize").asInt() * obj.get("fileSystemOffset").asInt()
                                            , true));
                            blockDevice.init();
                        }
                        return new Pair<>(Fat16FileSystem.read(blockDevice), obj);
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
    public IProducer<Pair<Fat16FileSystem, JsonObject>> makeFat16FileSystem() {
        return producer;
    }
}