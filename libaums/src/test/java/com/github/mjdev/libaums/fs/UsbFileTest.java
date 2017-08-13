package com.github.mjdev.libaums.fs;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.github.mjdev.libaums.util.Pair;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractTest;
import org.xenei.junit.contract.IProducer;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

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

        try {
            root.search(expectedValues.get("fileToCreateDirectoryOrFileOn").asString())
                    .search("should not happen");
            fail("UsbFile did not throw UnsupportedOperationException on search");
        } catch (UnsupportedOperationException e) {

        }
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
        assertEquals("Root getName", "/", root.getName());

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
        FileSystemFactory.setTimeZone(TimeZone.getTimeZone(expectedValues.get("timezone").asString()));
        JsonObject foldersToMove = expectedValues.get("createdAt").asObject();

        for (JsonObject.Member member : foldersToMove) {
            UsbFile file = root.search(member.getName());
            assertEquals(member.getName(), member.getValue().asLong(), file.createdAt() / 1000);
        }
    }

    @ContractTest
    public void lastModified() throws Exception {
        FileSystemFactory.setTimeZone(TimeZone.getTimeZone(expectedValues.get("timezone").asString()));

        JsonObject foldersToMove = expectedValues.get("lastModified").asObject();

        for (JsonObject.Member member : foldersToMove) {
            UsbFile file = root.search(member.getName());
            assertEquals(member.getName(), member.getValue().asLong(), file.lastModified() / 1000);
        }
    }

    @ContractTest
    public void lastAccessed() throws Exception {
        FileSystemFactory.setTimeZone(TimeZone.getTimeZone(expectedValues.get("timezone").asString()));

        JsonObject foldersToMove = expectedValues.get("lastAccessed").asObject();

        for (JsonObject.Member member : foldersToMove) {
            UsbFile file = root.search(member.getName());
            assertEquals(member.getName(), member.getValue().asLong(), file.lastAccessed() / 1000);
        }
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
                fail("Folder did not throw UnsupportedOperationException on getLength(): " + folder);
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
        UsbFile file = root.createFile("testlength");

        file.setLength(1337);
        assertEquals(1337, file.getLength());

        newInstance();

        assertEquals(1337, file.getLength());

        file = root.search("testlength");

        file.setLength(1134571);
        assertEquals(1134571, file.getLength());

        newInstance();

        assertEquals(1134571, file.getLength());

        UsbFile dir = root.createDirectory("my dir");

        try {
            dir.setLength(1337);
            fail("Directory did not throw UnsupportedOperationException on setLength");
        } catch (UnsupportedOperationException e) {

        }

    }

    @ContractTest
    public void read() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(19);

        UsbFile file = root.search(expectedValues.get("fileToRead").asString());

        file.read(0, buffer);

        assertEquals(buffer.capacity(), buffer.limit());
        assertEquals("this is just a test", new String(buffer.array()));

        JsonObject bigFileToRead = expectedValues.get("bigFileToRead").asObject();

        for(JsonObject.Member member : bigFileToRead) {
            String path = member.getName();
            file = root.search(path);
            URL url = new URL(member.getValue().asString());

            assertTrue(IOUtils.contentEquals(url.openStream(), new UsbFileInputStream(file)));
        }

        UsbFile dir = root.createDirectory("my dir");

        try {
            dir.read(0, buffer);
            fail("Directory did not throw UnsupportedOperationException on read");
        } catch (UnsupportedOperationException e) {

        }
    }

    @ContractTest
    public void write() throws Exception {
        // TODO test exception when disk is full
        URL bigFileUrl = new URL(expectedValues.get("bigFileToWrite").asString());
        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.put("this is just a test!".getBytes());
        buffer.flip();

        UsbFile file = root.createFile("writetest");

        file.write(0, buffer);

        buffer.flip();
        file.read(0, buffer);
        buffer.flip();
        byte[] dst = new byte[20];
        buffer.get(dst);
        assertEquals("this is just a test!", new String(dst));

        UsbFile bigFile = root.createFile("bigwritetest");
        IOUtils.copy(bigFileUrl.openStream(), new UsbFileOutputStream(bigFile));

        IOUtils.contentEquals(bigFileUrl.openStream(), new UsbFileInputStream(bigFile));

        newInstance();

        file = root.search("writetest");
        buffer.flip();
        file.read(0, buffer);
        buffer.flip();
        buffer.get(dst);
        assertEquals("this is just a test!", new String(dst));

        bigFile = root.search("bigwritetest");

        IOUtils.contentEquals(bigFileUrl.openStream(), new UsbFileInputStream(bigFile));
    }

    @ContractTest
    public void flush() throws Exception {
        // TODO
    }

    @ContractTest
    public void close() throws Exception {
        // TODO
    }

    @ContractTest
    public void createDirectory() throws Exception {
        UsbFile directory = root.createDirectory("new dir");
        UsbFile subDir = directory.createDirectory("new subdir");

        assertTrue(root.search(directory.getName()).isDirectory());
        assertTrue(root.search(directory.getName() + UsbFile.separator + subDir.getName()).isDirectory());

        newInstance();

        assertTrue(root.search(directory.getName()).isDirectory());
        assertTrue(root.search(directory.getName() + UsbFile.separator + subDir.getName()).isDirectory());

        try {
            root.search(expectedValues.get("fileToCreateDirectoryOrFileOn").asString())
                    .createDirectory("should not happen");
            fail("UsbFile did not throw UnsupportedOperationException on createDirectory");
        } catch (UnsupportedOperationException e) {

        }

        try {
            root.createDirectory(directory.getName());
            fail("UsbFile did not throw IOException when creating same name dir");
        } catch (IOException e) {

        }
    }

    @ContractTest
    public void createFile() throws Exception {
        UsbFile file = root.createFile("new file");
        UsbFile subFile = root.search(expectedValues.get("createFileInDir").asString()).
                createFile("new file");
        UsbFile specialCharFile = root.createFile("as~!@#$%^&()_-{},.=[]`'öäL@=(!\\\"&$%(!$)asdqweasdqweasd111°!§!§`´´");
        UsbFile specialCharFile2 = root.createFile("as~!@#$%^&()_-{},.=[]`'öäL@=(!\\\"&$%(!$)asdqweasdqweasd111°!§!§`´´2");

        assertFalse(root.search(file.getName()).isDirectory());
        assertFalse(root.search(specialCharFile.getName()).isDirectory());
        assertFalse(root.search(specialCharFile2.getName()).isDirectory());
        assertFalse(root.search(expectedValues.get("createFileInDir").asString() + UsbFile.separator + subFile.getName()).isDirectory());

        newInstance();

        assertFalse(root.search(file.getName()).isDirectory());
        assertFalse(root.search("as~!@#$%^&()_-{},.=[]`'öäL@=(!\\\"&$%(!$)asdqweasdqweasd111°!§!§`´´").isDirectory());
        assertFalse(root.search("as~!@#$%^&()_-{},.=[]`'öäL@=(!\\\"&$%(!$)asdqweasdqweasd111°!§!§`´´2").isDirectory());
        assertFalse(root.search(expectedValues.get("createFileInDir").asString() + UsbFile.separator + subFile.getName()).isDirectory());

        try {
            root.search(expectedValues.get("fileToCreateDirectoryOrFileOn").asString())
                    .createFile("should not happen");
            fail("UsbFile did not throw UnsupportedOperationException on createFile");
        } catch (UnsupportedOperationException e) {

        }

        try {
            root.createFile(file.getName());
            fail("UsbFile did not throw IOException when creating same name file");
        } catch (IOException e) {

        }
    }

    @ContractTest
    public void moveFile() throws Exception {
        JsonObject filesToMove = expectedValues.get("filesToMove").asObject();

        for (JsonObject.Member member : filesToMove) {
            UsbFile file = root.search(member.getName());
            UsbFile dest = root.search(member.getValue().asString());
            file.moveTo(dest);


            // try to move dir into file
            try {
                dest.moveTo(file);
                fail("Moving into file did not throw IllegalStateException");
            } catch (IllegalStateException e) {

            }

            // try to move file into file
            try {
                file.moveTo(file);
                fail("Moving into file did not throw IllegalStateException");
            } catch (IllegalStateException e) {

            }
        }

        for (JsonObject.Member member : filesToMove) {
            UsbFile file = root.search(member.getName());
            UsbFile dest = root.search(member.getValue().asString());
            assertNull(file);

            String path = member.getName();
            int lastSep = path.lastIndexOf(UsbFile.separator);
            if (lastSep == -1) lastSep = 0;
            assertFalse(dest.search(path.substring(lastSep))
                    .isDirectory());
        }

        newInstance();

        for (JsonObject.Member member : filesToMove) {
            UsbFile file = root.search(member.getName());
            UsbFile dest = root.search(member.getValue().asString());
            assertNull(file);

            String path = member.getName();
            int lastSep = path.lastIndexOf(UsbFile.separator);
            if (lastSep == -1) lastSep = 0;
            file = dest.search(path.substring(lastSep));
            assertFalse(file.isDirectory());
        }

    }

    @ContractTest
    public void moveDirectory() throws Exception {
        JsonObject foldersToMove = expectedValues.get("foldersToMove").asObject();

        for (JsonObject.Member member : foldersToMove) {
            UsbFile file = root.search(member.getName());
            UsbFile dest = root.search(member.getValue().asString());
            file.moveTo(dest);
        }

        for (JsonObject.Member member : foldersToMove) {
            UsbFile file = root.search(member.getName());
            UsbFile dest = root.search(member.getValue().asString());
            assertNull(file);

            String path = member.getName();
            int lastSep = path.lastIndexOf(UsbFile.separator);
            if (lastSep == -1) lastSep = 0;
            assertTrue(dest.search(path.substring(lastSep))
                    .isDirectory());
        }

        newInstance();

        UsbFile lastDest = null;
        for (JsonObject.Member member : foldersToMove) {
            UsbFile file = root.search(member.getName());
            UsbFile dest = root.search(member.getValue().asString());
            assertNull(file);

            String path = member.getName();
            int lastSep = path.lastIndexOf(UsbFile.separator);
            if (lastSep == -1) lastSep = 0;
            assertTrue(dest.search(path.substring(lastSep))
                    .isDirectory());

            lastDest = dest;
        }

        // try to move root dir
        try {
            root.moveTo(lastDest);
            fail("Moving root dir did not throw IllegalStateException");
        } catch (IllegalStateException e) {

        }

        cleanup();
        newInstance();
    }

    @ContractTest
    public void delete() throws Exception {
        UsbFile fileToDelete = root.search(expectedValues.get("fileToDelete").asString());
        UsbFile folderToDelete = root.search(expectedValues.get("folderToDelete").asString());

        fileToDelete.delete();
        folderToDelete.delete();

        assertNull(root.search(expectedValues.get("fileToDelete").asString()));
        assertNull(root.search(expectedValues.get("folderToDelete").asString()));

        newInstance();

        assertNull(root.search(expectedValues.get("fileToDelete").asString()));
        assertNull(root.search(expectedValues.get("folderToDelete").asString()));
    }

    @ContractTest
    public void deleteAll() throws Exception {
        String path = expectedValues.get("subDeleteAll").asString();
        UsbFile subDeleteAllFolder = root.search(path);
        int parentCount = subDeleteAllFolder.getParent().list().length;

        for (UsbFile file : subDeleteAllFolder.listFiles()) {
            file.delete();
        }

        assertEquals(parentCount, subDeleteAllFolder.getParent().list().length);
        assertEquals(0, subDeleteAllFolder.list().length);

        newInstance();

        subDeleteAllFolder = root.search(path);
        assertEquals(parentCount, subDeleteAllFolder.getParent().list().length);
        assertEquals(0, subDeleteAllFolder.list().length);

        newInstance();

        for (UsbFile file : root.listFiles()) {
            file.delete();
        }

        assertEquals(0, root.list().length);
        newInstance();
        assertEquals(0, root.list().length);
    }

    @ContractTest
    public void isRoot() throws Exception {

        assertTrue(root.isRoot());

        for (UsbFile file : root.listFiles()) {
            assertFalse(file.isRoot());
        }
    }

    private void checkAbsolutePathRecursive(String currentDir, UsbFile dir) throws IOException {
        for (UsbFile file : dir.listFiles()) {
            String test = currentDir + UsbFile.separator + file.getName();
            if (currentDir.equals(UsbFile.separator)) {
                test = UsbFile.separator + file.getName();
            }

            assertEquals(test, file.getAbsolutePath());

            if (file.isDirectory()) {
                String nextDir = currentDir + UsbFile.separator + file.getName();
                if (currentDir.equals(UsbFile.separator)) {
                    nextDir = UsbFile.separator + file.getName();
                }
                checkAbsolutePathRecursive(nextDir, file);
            }
        }
    }

    @ContractTest
    public void absolutePath() throws Exception {
        checkAbsolutePathRecursive(UsbFile.separator, root);
    }

    private void checkEqualsRecursive(UsbFile dir) throws IOException {
        for (UsbFile file : dir.listFiles()) {
            if (file.isDirectory()) {
                checkEqualsRecursive(file);
            }

            assertEquals(file, file);
        }
    }

    @ContractTest
    public void equals() throws Exception {
        checkEqualsRecursive(root);
    }

}