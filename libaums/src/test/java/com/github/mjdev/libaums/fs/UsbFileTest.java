package com.github.mjdev.libaums.fs;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.github.mjdev.libaums.util.Pair;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractTest;
import org.xenei.junit.contract.IProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    public void testSingleFileReferenceSearch() throws IOException {
        for (JsonValue value : expectedValues.get("search").asArray()) {
            String path = value.asString();
            assertSame(root.search(path), root.search(path));
        }
    }

    @ContractTest
    public void testSingleFileReferenceCreate() throws IOException {
        assertSame(root.createDirectory("ref_test"), root.search("ref_test"));
        assertSame(root.createFile("ref_test.txt"), root.search("ref_test.txt"));

        UsbFile file = root.createDirectory("test_single_ref").createDirectory("sub").createFile("file.txt");
        assertSame(file, root.search("test_single_ref/sub/file.txt"));
        assertSame(file.getParent(), root.search("/test_single_ref/sub/"));
        assertSame(file.getParent().getParent(), root.search("test_single_ref/"));

        newInstance();

        assertNotSame(file, root.search("test_single_ref/sub/file.txt"));
        assertNotSame(file.getParent(), root.search("/test_single_ref/sub/"));
        assertNotSame(file.getParent().getParent(), root.search("test_single_ref/"));
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

        int numberOfFiles = root.listFiles().length;
        String[] files = root.list();

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

        // do that again to check LRU cache of FAT
        file = root.search(expectedValues.get("fileToRead").asString());

        file.read(0, buffer);
        file.flush();

        assertEquals(buffer.capacity(), buffer.limit());

        for(JsonObject.Member member : bigFileToRead) {
            String path = member.getName();
            file = root.search(path);
            URL url = new URL(member.getValue().asString());

            assertTrue(IOUtils.contentEquals(url.openStream(), new UsbFileInputStream(file)));
        }

        file.flush();

        assertArrayEquals(files, root.list());
        assertEquals(numberOfFiles, root.listFiles().length);

        newInstance();

        assertArrayEquals(files, root.list());
        assertEquals(numberOfFiles, root.listFiles().length);


        UsbFile dir = root.createDirectory("my dir");

        try {
            dir.read(0, buffer);
            fail("Directory did not throw UnsupportedOperationException on read");
        } catch (UnsupportedOperationException e) {

        }

    }

    /**
     * shamelessly stolen from IOUtils of apache commons to increase buffer size
     * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method uses the provided buffer, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output  the <code>OutputStream</code> to write to
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
     * @since 2.2
     */
    static long copyLarge(InputStream input, OutputStream output, byte[] buffer)
            throws IOException {
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        output.close();
        return count;
    }

    @ContractTest
    public void write() throws Exception {

        int numberOfFiles = root.listFiles().length;

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

        assertTrue(IOUtils.contentEquals(bigFileUrl.openStream(), new UsbFileInputStream(bigFile)));

        UsbFile bigFileLargeBuffer = root.createFile("bigwritetestlargebuffer");
        copyLarge(new UsbFileInputStream(bigFile),
                new UsbFileOutputStream(bigFileLargeBuffer), new byte[7 * 32768]);


        assertTrue(IOUtils.contentEquals(bigFileUrl.openStream(), new UsbFileInputStream(bigFileLargeBuffer)));

        assertEquals(numberOfFiles + 3, root.listFiles().length);

        newInstance();

        file = root.search("writetest");
        buffer.flip();
        file.read(0, buffer);
        buffer.flip();
        buffer.get(dst);
        assertEquals("this is just a test!", new String(dst));

        bigFile = root.search("bigwritetest");

        assertTrue(IOUtils.contentEquals(bigFileUrl.openStream(), new UsbFileInputStream(bigFile)));


        bigFile = root.search("bigwritetestlargebuffer");

        assertEquals(bigFileLargeBuffer.getLength(), bigFile.getLength());

        assertTrue(IOUtils.contentEquals(bigFileUrl.openStream(), new UsbFileInputStream(bigFile)));

        assertEquals(numberOfFiles + 3, root.listFiles().length);
    }

    @ContractTest
    public void writeWithLength() throws Exception {

        int numberOfFiles = root.listFiles().length;

        URL bigFileUrl = new URL(expectedValues.get("bigFileToWrite").asString());

        UsbFile bigFileLargeBuffer = root.createFile("bigwritetestlargebuffer");
        bigFileLargeBuffer.setLength(29876);
        copyLarge(bigFileUrl.openStream(),
                new UsbFileOutputStream(bigFileLargeBuffer), new byte[7 * 32768]);


        assertTrue(IOUtils.contentEquals(bigFileUrl.openStream(), new UsbFileInputStream(bigFileLargeBuffer)));

        newInstance();

        UsbFile bigFile = root.search("bigwritetestlargebuffer");

        assertEquals(bigFileLargeBuffer.getLength(), bigFile.getLength());

        assertTrue(IOUtils.contentEquals(bigFileUrl.openStream(), new UsbFileInputStream(bigFile)));

        assertEquals(numberOfFiles + 1, root.listFiles().length);
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

        assertEquals("/", root.getAbsolutePath());

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

    @ContractTest
    public void createLotsOfFiles() throws IOException {
        UsbFile dir = root.createDirectory("test_lots_of_files");
        List<String> nameList = new ArrayList<>();

        for(int i = 0; i < 600; i++) {
            String name = String.format("IMG_09082016_%06d", i);
            nameList.add(name);
            dir.createFile(name);
        }
        assertEquals(nameList.size(), dir.list().length);
        assertArrayEquals(nameList.toArray(new String[0]), dir.list());

        for(int j = 0; j < 12; j++) {
            dir = root.createDirectory("test_lots_of_files_" + j);

            for(int i = 0; i < 600; i++) {
                String name = String.format("IMG_09082016_%06d", i);
                dir.createFile(name);
            }
            assertEquals(nameList.size(), dir.list().length);
            assertArrayEquals(nameList.toArray(new String[0]), dir.list());
        }

        newInstance();

        dir = root.search("test_lots_of_files");

        assertEquals(nameList.size(), dir.list().length);
        assertArrayEquals(nameList.toArray(new String[0]), dir.list());

        for(int j = 0; j < 12; j++) {
            dir = root.search("test_lots_of_files_" + j);
            assertEquals(nameList.size(), dir.list().length);
            assertArrayEquals(nameList.toArray(new String[0]), dir.list());
        }
    }

    @ContractTest
    public void testIssue187() throws IOException {
        UsbFile file = root.createFile("testissue187");
        OutputStream outputStream = UsbFileStreamFactory.createBufferedOutputStream(file, fs);
        outputStream.write("START\n".getBytes());
        int i;

        for (i = 6; i < 40000; i += 5) {
            outputStream.write("TEST\n".getBytes());
        }

        outputStream.write("END\n".getBytes());
        outputStream.close();


        UsbFile srcPtr = root.search("testissue187");
        long srcLen = srcPtr.getLength();
        UsbFile dstPtr = root.createFile("testissue187_copy");
        InputStream inputStream = UsbFileStreamFactory.createBufferedInputStream(srcPtr, fs);
        OutputStream outStream  = UsbFileStreamFactory.createBufferedOutputStream(dstPtr, fs);

        byte[] bytes = new byte[fs.getChunkSize()];

        dstPtr.setLength(srcLen);

        int count;
        while ((count=inputStream.read(bytes))>0) {
            outStream.write(bytes,0,count);
        }
        inputStream.close();
        outStream.close();


        InputStream inputStream1 = UsbFileStreamFactory.createBufferedInputStream(srcPtr, fs);
        InputStream inputStream2  = UsbFileStreamFactory.createBufferedInputStream(dstPtr, fs);
        assertTrue(IOUtils.contentEquals(inputStream1, inputStream2));
    }

    @ContractTest
    public void testIssue215() throws IOException {
        
        UsbFile file1b1 = getFile(root, "Folder1a/Folder1b/File1b1.txt");
        UsbFile file1b2 = getFile(root, "Folder1a/Folder1b/File1b2.txt");
        UsbFile file1b3 = getFile(root, "Folder1a/Folder1b/File1b3.txt");
        UsbFile file2a = getFile(root, "Folder2a/File2a.txt");
        UsbFile file1 = getFile(root, "File1.txt");

        OutputStream outputStream = new UsbFileOutputStream(file1b1);
        outputStream.write(file1b1.getName().getBytes());
        outputStream.close();
        outputStream = new UsbFileOutputStream(file1b2);
        outputStream.write(file1b2.getName().getBytes());
        outputStream.close();
        outputStream = new UsbFileOutputStream(file1b3);
        outputStream.write(file1b3.getName().getBytes());
        outputStream.close();
        outputStream = new UsbFileOutputStream(file2a);
        outputStream.write(file2a.getName().getBytes());
        outputStream.close();
        outputStream = new UsbFileOutputStream(file1);
        outputStream.write(file1.getName().getBytes());
        outputStream.close();

        assertSame(file1b1.getParent(), file1b2.getParent());
        assertSame(file1b1.getParent(), file1b2.getParent());

        assertNotNull(root.search("Folder1a/Folder1b/File1b1.txt"));
        assertNotNull(root.search("Folder1a/Folder1b/File1b2.txt"));
        assertNotNull(root.search("Folder1a/Folder1b/File1b3.txt"));

        assertTrue(IOUtils.contentEquals(new UsbFileInputStream(root.search("Folder1a/Folder1b/File1b3.txt")),
                new ByteArrayInputStream("File1b3.txt".getBytes(StandardCharsets.UTF_8))));

        assertTrue(IOUtils.contentEquals(new UsbFileInputStream(root.search("Folder1a/Folder1b/File1b1.txt")),
                new ByteArrayInputStream("File1b1.txt".getBytes(StandardCharsets.UTF_8))));

        assertTrue(IOUtils.contentEquals(new UsbFileInputStream(root.search("Folder1a/Folder1b/File1b2.txt")),
                new ByteArrayInputStream("File1b2.txt".getBytes(StandardCharsets.UTF_8))));

        newInstance();

        assertNotNull(root.search("Folder1a/Folder1b/File1b1.txt"));
        assertNotNull(root.search("Folder1a/Folder1b/File1b2.txt"));
        assertNotNull(root.search("Folder1a/Folder1b/File1b3.txt"));

        assertTrue(IOUtils.contentEquals(new UsbFileInputStream(root.search("Folder1a/Folder1b/File1b1.txt")),
                new ByteArrayInputStream("File1b1.txt".getBytes(StandardCharsets.UTF_8))));

        assertTrue(IOUtils.contentEquals(new UsbFileInputStream(root.search("Folder1a/Folder1b/File1b2.txt")),
                new ByteArrayInputStream("File1b2.txt".getBytes(StandardCharsets.UTF_8))));

        assertTrue(IOUtils.contentEquals(new UsbFileInputStream(root.search("Folder1a/Folder1b/File1b3.txt")),
                new ByteArrayInputStream("File1b3.txt".getBytes(StandardCharsets.UTF_8))));
    }
    
    private static UsbFile getFile(UsbFile root, String path) throws IOException {
        String[] items = path.split("/");

        UsbFile child = root;
        for (int i=0; i<items.length; ++i) {
            UsbFile next = child.search(items[i]);
            if (next == null) {
                for (; i < items.length - 1; ++i)
                    child = child.createDirectory(items[i]);
                child = child.createFile(items[i++]);
            } else {
                child = next;
            }
        }

        return child;
    }

}
