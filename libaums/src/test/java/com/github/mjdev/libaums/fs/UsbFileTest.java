package com.github.mjdev.libaums.fs;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.github.mjdev.libaums.util.Pair;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractTest;
import org.xenei.junit.contract.IProducer;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by magnusja on 03/08/17.
 */
@Contract(UsbFile.class)
public class UsbFileTest {

    private IProducer<Pair<FileSystem, JsonObject>> producer;
    private FileSystem fs;
    private JsonObject expectedValues;

    @Contract.Inject
    public void setFileSystem(IProducer<Pair<FileSystem, JsonObject>> producer) {
        this.producer = producer;
    }

    @Before
    public void setUp() {
        newInstance();
    }

    @After
    public void cleanup() {
        producer.cleanUp();
    }

    private void newInstance() {
        Pair<FileSystem, JsonObject> pair = producer.newInstance();
        fs = pair.getLeft();
        expectedValues = pair.getRight();
    }

    @ContractTest
    public void search() throws Exception {
        UsbFile root = fs.getRootDirectory();

        for (JsonValue value : expectedValues.get("search").asArray()) {
            String path = value.asString();
            Assert.assertNotNull(path, root.search(path));
        }
        String garbagePath = "garbage path!)(&`";
        Assert.assertNull(garbagePath, root.search(garbagePath));
    }

    @ContractTest
    public void isDirectory() throws Exception {
        UsbFile root = fs.getRootDirectory();
        for (JsonObject.Member member: expectedValues.get("isDirectory").asObject()) {
            String path = member.getName();
            Assert.assertEquals(path, member.getValue().asBoolean(), root.search(path).isDirectory());
        }
    }

    @Test
    public void getName() throws Exception {

    }

    @Test
    public void setName() throws Exception {

    }

    @Test
    public void createdAt() throws Exception {

    }

    @Test
    public void lastModified() throws Exception {

    }

    @Test
    public void lastAccessed() throws Exception {

    }

    @Test
    public void getParent() throws Exception {

    }

    @Test
    public void list() throws Exception {

    }

    @Test
    public void listFiles() throws Exception {

    }

    @Test
    public void getLength() throws Exception {

    }

    @Test
    public void setLength() throws Exception {

    }

    @Test
    public void read() throws Exception {

    }

    @Test
    public void write() throws Exception {

    }

    @Test
    public void flush() throws Exception {

    }

    @Test
    public void close() throws Exception {

    }

    @Test
    public void createDirectory() throws Exception {

    }

    @Test
    public void createFile() throws Exception {

    }

    @Test
    public void moveTo() throws Exception {

    }

    @Test
    public void delete() throws Exception {

    }

    @Test
    public void isRoot() throws Exception {

    }

}