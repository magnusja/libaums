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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by magnusja on 03/08/17.
 */
@Contract(UsbFile.class)
public class UsbFileTest {

    private IProducer<Pair<FileSystem, JsonObject>> producer;
    private FileSystem fs;
    private UsbFile root;
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
        root = fs.getRootDirectory();
        expectedValues = pair.getRight();
    }

    @ContractTest
    public void search() throws Exception {
        for (JsonValue value : expectedValues.get("search").asArray()) {
            String path = value.asString();
            Assert.assertNotNull(path, root.search(path));
        }
        String garbagePath = "garbage path!)(&`";
        assertNull(garbagePath, root.search(garbagePath));
    }

    @ContractTest
    public void isDirectory() throws Exception {
        for (JsonObject.Member member: expectedValues.get("isDirectory").asObject()) {
            String path = member.getName();
            assertEquals(path, member.getValue().asBoolean(), root.search(path).isDirectory());
        }
    }

    @ContractTest
    public void getName() throws Exception {
        assertEquals("Root getName", "", root.getName());

        for (JsonValue value : expectedValues.get("getName").asArray()) {
            String filePath = value.asString();
            assertEquals("Get Name", filePath, root.search(filePath).getName());
        }
    }

    @ContractTest
    public void setName() throws Exception {
        JsonArray oldNames = expectedValues.get("getName").asArray();
        JsonArray newNames = expectedValues.get("setName").asArray();

        int i = 0;
        for (JsonValue value : newNames) {
            String newName = value.asString();
            String oldName = oldNames.get(i).asString();
            i++;

            UsbFile file = root.search(oldName);
            file.setName(newName);
            assertEquals(newName, file.getName());
        }

        // force reread
        newInstance();

        for (JsonValue value : newNames) {
            String filePath = value.asString();
            assertEquals(filePath, root.search(filePath).getName());
        }
    }

    @ContractTest
    public void createdAt() throws Exception {

    }

    @ContractTest
    public void lastModified() throws Exception {

    }

    @ContractTest
    public void lastAccessed() throws Exception {

    }

    @ContractTest
    public void getParent() throws Exception {
        UsbFile root = fs.getRootDirectory();
        assertNull(root.getParent());

        for (UsbFile file : root.listFiles()) {
            assertSame(root, file.getParent());

            if (file.isDirectory()) {
                for (UsbFile file2 : file.listFiles()) {
                    assertSame(file, file2.getParent());
                }
            }
        }
    }

    @ContractTest
    public void list() throws Exception {
        JsonObject listedFolders = expectedValues.get("list").asObject();

        for (JsonObject.Member member : listedFolders) {
            String toList = member.getName();
            JsonArray listed = member.getValue().asArray();

            String[] actualList = root.search(toList).list();

            int count = 0;
            for (String fileName : actualList) {
                if (!fileName.startsWith(".")) {
                    count++;
                }
            }

            assertEquals(listed.size(), count);

            for (JsonValue fileName : listed) {
                assertTrue(Arrays.asList(actualList).contains(fileName.asString()));
            }
        }
    }

    @ContractTest
    public void listFiles() throws Exception {
        JsonObject listedFolders = expectedValues.get("list").asObject();

        for (JsonObject.Member member : listedFolders) {
            String toList = member.getName();
            JsonArray listed = member.getValue().asArray();

            UsbFile[] actualList = root.search(toList).listFiles();
            List<String> names = new ArrayList<>();

            int count = 0;
            for (UsbFile file : actualList) {
                names.add(file.getName());
                if (!file.getName().startsWith(".")) {
                    count++;
                }
            }

            assertEquals(listed.size(), count);

            for (JsonValue fileName : listed) {
                assertTrue(names.contains(fileName.asString()));
            }
        }
    }

    @ContractTest
    public void getLengthFolder() throws Exception {
        JsonArray folders = expectedValues.get("getLengthFolders").asArray();
        for (JsonValue value : folders) {
            String folder = value.asString();
            try {
                root.search(folder).getLength();
                fail();
            } catch(UnsupportedOperationException e) {

            }
        }
    }

    @ContractTest
    public void getLengthFile() throws Exception {
        for (JsonObject.Member member: expectedValues.get("getLengthFiles").asObject()) {
            String path = member.getName();
            assertEquals(path, member.getValue().asInt(), root.search(path).getLength());
        }
    }

    @ContractTest
    public void setLength() throws Exception {

    }

    @ContractTest
    public void read() throws Exception {

    }

    @ContractTest
    public void write() throws Exception {

    }

    @ContractTest
    public void flush() throws Exception {

    }

    @ContractTest
    public void close() throws Exception {

    }

    @ContractTest
    public void createDirectory() throws Exception {

    }

    @ContractTest
    public void createFile() throws Exception {

    }

    @ContractTest
    public void moveTo() throws Exception {

    }

    @ContractTest
    public void delete() throws Exception {

    }

    @ContractTest
    public void isRoot() throws Exception {

        assertTrue(root.isRoot());

        for (UsbFile file : root.listFiles()) {
            assertFalse(file.isRoot());
        }
    }

}