/*
 * (C) Copyright 2014-2016 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.mjdev.libaums.fs.fat16;

import android.util.Log;

import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.AbstractUsbFile;
import com.github.mjdev.libaums.fs.UsbFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class represents a directory in the Fat16 file system. It can hold other
 * directories and files.
 *
 * @author mjahnen
 */
public class FatDirectory extends AbstractUsbFile {

    private static String TAG = FatDirectory.class.getSimpleName();

    private BlockDeviceDriver blockDevice;
    private FAT fat;
    private Fat16BootSector bootSector;
    /**
     * Entries read from the device.
     */
    private List<FAT16LongNameEntry> entries;
    /**
     * Map for checking for existence when for example creating new files or
     * directories.
     * <p>
     * All items are stored in lower case because a Fat16 fs is not case
     * sensitive.
     */
    private Map<String, FAT16LongNameEntry> lfnMap;
    /**
     * Map for checking for existence of short names when generating short names
     * for new files or directories.
     */
    private Map<ShortName, FatDirectoryEntry> shortNameMap;
    /**
     * Null if this is the root directory.
     */
    private FatDirectory parent;
    /**
     * Null if this is the root directory.
     */
    private FAT16LongNameEntry entry;

    private String volumeLabel;

    private boolean hasBeenInited;

    /**
     * Constructs a new FatDirectory with the given information.
     *
     * @param blockDevice The block device the fs is located.
     * @param fat         The FAT of the fs.
     * @param bootSector  The boot sector if the fs.
     * @param parent      The parent directory of the newly created one.
     */
    private FatDirectory(BlockDeviceDriver blockDevice, FAT fat, Fat16BootSector bootSector,
                         FatDirectory parent) {
        this.blockDevice = blockDevice;
        this.fat = fat;
        this.bootSector = bootSector;
        this.parent = parent;
        lfnMap = new HashMap<String, FAT16LongNameEntry>();
        shortNameMap = new HashMap<ShortName, FatDirectoryEntry>();
    }

    /**
     * This method creates a new directory from a given
     * {@link FatDirectoryEntry}.
     *
     * @param entry       The entry of the directory.
     * @param blockDevice The block device the fs is located.
     * @param fat         The FAT of the fs.
     * @param bootSector  The boot sector if the fs.
     * @param parent      The parent directory of the newly created one.
     * @return Newly created directory.
     */
    /* package */
    static FatDirectory create(FAT16LongNameEntry entry,
                               BlockDeviceDriver blockDevice, FAT fat, Fat16BootSector bootSector, FatDirectory parent) {
        FatDirectory result = new FatDirectory(blockDevice, fat, bootSector, parent);
        result.entry = entry;
        return result;
    }

    /**
     * Reads the root directory from a Fat16 file system.
     *
     * @param blockDevice The block device the fs is located.
     * @param fat         The FAT of the fs.
     * @param bootSector  The boot sector if the fs.
     * @return Newly created root directory.
     * @throws IOException If reading from the device fails.
     */
    /* package */
    static FatDirectory readRoot(BlockDeviceDriver blockDevice, FAT fat,
                                 Fat16BootSector bootSector) throws IOException {
        FatDirectory result = new FatDirectory(blockDevice, fat, bootSector, null);
        result.init(); // init calls readEntries
        return result;
    }

    /**
     * Initializes the {@link FatDirectory}. Creates the cluster chain if needed
     * and reads all entries from the cluster chain.
     *
     * @throws IOException If reading from the device fails.
     */
    private void init() throws IOException {

        // entries is allocated here
        // an exception will be thrown if entries is used before the directory has been initialised
        // use of uninitialised entries can lead to data loss!
        if (entries == null) {
            entries = new ArrayList<FAT16LongNameEntry>();
        }

        // only read entries if we have no entries
        // otherwise newly created directories (. and ..) will read trash data
        if (entries.size() == 0 && !hasBeenInited) {
            readEntries();
        }

        hasBeenInited = true;
    }

    /**
     * Reads all entries from the directory and saves them into {@link #lfnMap},
     * {@link #entries} and {@link #shortNameMap}.
     *
     * @throws IOException If reading from the device fails.
     * @see #write()
     */
    private void readEntries() throws IOException {
        if (isRoot()) {
            long rootDirStartSector = bootSector.getRootDirStartSector();
            long dataBlockSectorStart = bootSector.getDataStartSector();

            long currentSector = rootDirStartSector;


            List<FatDirectoryEntry> entries = new ArrayList<>();
            ByteBuffer bb = UnsignedUtil.allocateLittleEndian(512);
            while (currentSector < dataBlockSectorStart) {


                bb.position(0);
                blockDevice.read(currentSector * 512, bb);
                bb.flip();
                for (int x = 0; x < 16; x++) {
                    byte[] record = new byte[32];
                    bb.get(record);

                    FatDirectoryEntry read = FatDirectoryEntry.read(ByteBuffer.wrap(record));
                    if (read != null)
                        entries.add(read);
                }

                currentSector++;
            }

            List<FatDirectoryEntry> lfns = new ArrayList<>();
            for (FatDirectoryEntry entry : entries) {
                if (entry.isLfnEntry()) {
                    lfns.add(entry);
                } else {
                    addEntry(FAT16LongNameEntry.read(entry, lfns), entry);
                    lfns.clear();
                }
            }
        } else {

            Long[] chain = fat.getChain(entry.getActualEntry().getFirstFATCluster());

            List<FatDirectoryEntry> entries = new ArrayList<>();
            ByteBuffer bb = UnsignedUtil.allocateLittleEndian(512);
            for (Long byteLocation : chain) {

                bb.position(0);
                blockDevice.read(bootSector.getDataAreaOffset() + ((byteLocation - 2) * bootSector.getSectorsPerCluster() * bootSector.getBytesPerCluster()), bb);
                bb.flip();
                for (int x = 0; x < 16; x++) {
                    byte[] record = new byte[32];
                    bb.get(record);

                    FatDirectoryEntry read = FatDirectoryEntry.read(ByteBuffer.wrap(record));
                    if (read != null)
                        entries.add(read);
                }
            }

            List<FatDirectoryEntry> lfns = new ArrayList<>();
            for (FatDirectoryEntry entry : entries) {
                if (entry.isLfnEntry()) {
                    lfns.add(entry);
                } else {
                    addEntry(FAT16LongNameEntry.read(entry, lfns), entry);
                    lfns.clear();
                }
            }
        }

    }

    /**
     * Adds the long file name entry to {@link #lfnMap} and {@link #entries} and
     * the actual entry to {@link #shortNameMap}.
     * <p>
     * This method does not write the changes to the disk. If you want to do so
     * call {@link #write()} after adding an entry.
     *
     * @param lfnEntry The long filename entry to add.
     * @param entry    The corresponding short name entry.
     * @see #removeEntry(FAT16LongNameEntry)
     */
    private void addEntry(FAT16LongNameEntry lfnEntry, FatDirectoryEntry entry) {
        entries.add(lfnEntry);
        lfnMap.put(lfnEntry.getName().toLowerCase(Locale.getDefault()), lfnEntry);
        shortNameMap.put(entry.getShortName(), entry);
    }

    /**
     * Removes (if existing) the long file name entry from {@link #lfnMap} and
     * {@link #entries} and the actual entry from {@link #shortNameMap}.
     * <p>
     * This method does not write the changes to the disk. If you want to do so
     * call {@link #write()} after adding an entry.
     *
     * @param lfnEntry The long filename entry to remove.
     * @see #addEntry(FAT16LongNameEntry, FatDirectoryEntry)
     */
    /* package */void removeEntry(FAT16LongNameEntry lfnEntry) {
        entries.remove(lfnEntry);
        lfnMap.remove(lfnEntry.getName().toLowerCase(Locale.getDefault()));
        shortNameMap.remove(lfnEntry.getActualEntry().getShortName());
    }

    /**
     * Renames a long filename entry to the desired new name.
     * <p>
     * This method immediately writes the change to the disk, thus no further
     * call to {@link #write()} is needed.
     *
     * @param lfnEntry The long filename entry to rename.
     * @param newName  The new name.
     * @throws IOException If writing the change to the disk fails.
     */
    /* package */void renameEntry(FAT16LongNameEntry lfnEntry, String newName) throws IOException {
        if (lfnEntry.getName().equals(newName))
            return;

        removeEntry(lfnEntry);
        lfnEntry.setName(newName,
                ShortNameGenerator.generateShortName(newName, shortNameMap.keySet()));
        addEntry(lfnEntry, lfnEntry.getActualEntry());
        write();
    }

    /**
     * Writes the {@link #entries} to the disk. Any changes made by
     * {@link #addEntry(FAT16LongNameEntry, FatDirectoryEntry)} or
     * {@link #removeEntry(FAT16LongNameEntry)} will then be committed to the
     * device.
     *
     * @throws IOException
     * @see {@link #write()}
     */
    /* package */void write() throws IOException {

        if (isRoot()) {
            ByteBuffer allocate = UnsignedUtil.allocateLittleEndian(bootSector.getBytesPerCluster());

            for (int i = 0; i < entries.size(); i++) {
                FAT16LongNameEntry fat16LongNameEntry = entries.get(i);
                fat16LongNameEntry.serialize(allocate);
            }

            allocate.position(0);
            blockDevice.write(bootSector.getRootDirStartSector() * bootSector.getBytesPerSector(), allocate);

        } else {

            Long[] chain = fat.getChain(entry.getStartCluster());

            ByteBuffer allocate = UnsignedUtil.allocateLittleEndian(bootSector.getBytesPerCluster());

            int chainCounter = 0;

            for (int i = 0; i < entries.size(); i++) {


                if ((i * 32) % 512 == 0 && i > 0) {
                    allocate.position(0);
                    blockDevice.write(bootSector.getByteAddressForCluster(chain[chainCounter]), allocate);
                    allocate.position(0);
                    chainCounter++;
                }

                FAT16LongNameEntry fat16LongNameEntry = entries.get(i);
                fat16LongNameEntry.serialize(allocate);
            }

            if (allocate.position() > 0) {
                allocate.position(0);
                blockDevice.write(bootSector.getByteAddressForCluster(chain[chainCounter]), allocate);
            }
        }

    }

    /**
     * @return True if this directory is the root directory.
     */
    @Override
    public boolean isRoot() {
        return entry == null;
    }

    /**
     * This method returns the volume label which can be stored in the root
     * directory of a Fat16 file system.
     *
     * @return The volume label.
     */
    /* package */String getVolumeLabel() {
        return volumeLabel;
    }

    @Override
    public FatFile createFile(String name) throws IOException {
        if (lfnMap.containsKey(name.toLowerCase(Locale.getDefault())))
            throw new IOException("Item already exists!");

        init(); // initialise the directory before creating files

        ShortName shortName = ShortNameGenerator.generateShortName(name, shortNameMap.keySet());

        FAT16LongNameEntry entry = FAT16LongNameEntry.createNew(name, shortName);
        // alloc completely new chain
        long newStartCluster = fat.alloc(1)[0];
        entry.setStartCluster(newStartCluster);

        Log.d(TAG, "adding entry: " + entry + " with short name: " + shortName);
        addEntry(entry, entry.getActualEntry());
        // write changes immediately to disk
        write();

        return FatFile.create(entry, blockDevice, fat, bootSector, this);
    }

    @Override
    public FatDirectory createDirectory(String name) throws IOException {
        if (lfnMap.containsKey(name.toLowerCase(Locale.getDefault())))
            throw new IOException("Item " + name + " already exists! ");

        init(); // initialise the directory before creating files

        ShortName shortName = ShortNameGenerator.generateShortName(name, shortNameMap.keySet());

        FAT16LongNameEntry entry = FAT16LongNameEntry.createNew(name, shortName);
        entry.setDirectory();
        // alloc completely new chain
        long newStartCluster = fat.alloc(1)[0];
        entry.setStartCluster(newStartCluster);

        Log.d(TAG, "adding entry: " + entry + " with short name: " + shortName);
        addEntry(entry, entry.getActualEntry());
        // write changes immediately to disk
        write();


        FatDirectory result = FatDirectory.create(entry, blockDevice, fat, bootSector, this);
        result.hasBeenInited = true;
        result.entry.setStartCluster(newStartCluster);
        result.entries = new ArrayList<FAT16LongNameEntry>(); // initialise entries before adding sub-directories

        // first create the dot entry which points to the dir just created
        FAT16LongNameEntry dotEntry = FAT16LongNameEntry
                .createNew(null, new ShortName(".", ""));
        dotEntry.setDirectory();
        dotEntry.setStartCluster(newStartCluster);
        FAT16LongNameEntry.copyDateTime(entry, dotEntry);
        result.addEntry(dotEntry, dotEntry.getActualEntry());

        // Second the dotdot entry which points to the parent directory (this)
        // if parent is the root dir then set start cluster to zero
        FAT16LongNameEntry dotDotEntry = FAT16LongNameEntry.createNew(null, new ShortName("..",
                ""));
        dotDotEntry.setDirectory();
        dotDotEntry.setStartCluster(isRoot() ? 0 : this.entry.getStartCluster());
        FAT16LongNameEntry.copyDateTime(entry, dotDotEntry);
        result.addEntry(dotDotEntry, dotDotEntry.getActualEntry());

        // write changes immediately to disk
        result.write();

        return result;
    }

    @Override
    public void setLength(long newLength) {
        throw new UnsupportedOperationException("This is a directory!");
    }

    @Override
    public long getLength() {
        throw new UnsupportedOperationException("This is a directory!");
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public String getName() {
        return entry != null ? entry.getName() : "/";
    }

    @Override
    public void setName(String newName) throws IOException {
        if (isRoot())
            throw new IllegalStateException("Cannot rename root dir!");
        parent.renameEntry(entry, newName);
    }

    @Override
    public long createdAt() {
        if (isRoot())
            throw new IllegalStateException("root dir!");
        return entry.getActualEntry().getCreatedDateTime();
    }

    @Override
    public long lastModified() {
        if (isRoot())
            throw new IllegalStateException("root dir!");
        return entry.getActualEntry().getLastModifiedDateTime();
    }

    @Override
    public long lastAccessed() {
        if (isRoot())
            throw new IllegalStateException("root dir!");
        return entry.getActualEntry().getLastAccessedDateTime();
    }

    @Override
    public UsbFile getParent() {
        return parent;
    }

    @Override
    public String[] list() throws IOException {
        init();
        List<String> list = new ArrayList<String>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            String name = entries.get(i).getName();
            if (!name.equals(".") && !name.equals("..")) {
                list.add(name);
            }
        }

        String[] array = new String[list.size()];
        array = list.toArray(array);

        return array;
    }

    @Override
    public UsbFile[] listFiles() throws IOException {
        init();
        List<UsbFile> list = new ArrayList<UsbFile>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            FAT16LongNameEntry entry = entries.get(i);
            String name = entry.getName();
            if (name.equals(".") || name.equals(".."))
                continue;

            if (entry.isDirectory()) {
                list.add(FatDirectory.create(entry, blockDevice, fat, bootSector, this));
            } else {
                list.add(FatFile.create(entry, blockDevice, fat, bootSector, this));
            }
        }

        UsbFile[] array = new UsbFile[list.size()];
        array = list.toArray(array);

        return array;
    }

    @Override
    public void read(long offset, ByteBuffer destination) throws IOException {
        throw new UnsupportedOperationException("This is a directory!");
    }

    @Override
    public void write(long offset, ByteBuffer source) throws IOException {
        throw new UnsupportedOperationException("This is a directory!");
    }

    @Override
    public void flush() throws IOException {
        throw new UnsupportedOperationException("This is a directory!");
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("This is a directory!");
    }

    @Override
    public void moveTo(UsbFile destination) throws IOException {
        if (isRoot())
            throw new IllegalStateException("cannot move root dir!");

        if (!destination.isDirectory())
            throw new IllegalStateException("destination cannot be a file!");
        if (!(destination instanceof FatDirectory))
            throw new IllegalStateException("cannot move between different filesystems!");
        // TODO check if destination is really on the same physical device or
        // partition!

        FatDirectory destinationDir = (FatDirectory) destination;
        if (destinationDir.lfnMap.containsKey(entry.getName().toLowerCase(Locale.getDefault())))
            throw new IOException("item already exists in destination!");

        init();
        destinationDir.init();

        // now the actual magic happens!
        parent.removeEntry(entry);
        destinationDir.addEntry(entry, entry.getActualEntry());

        parent.write();
        destinationDir.write();
        parent = destinationDir;
    }

    /**
     * This method moves an long filename entry currently stored in THIS
     * directory to the destination which also must be a directory.
     * <p>
     * Used by {@link FatFile} to move itself to another directory.
     *
     * @param entry       The entry which shall be moved.
     * @param destination The destination directory.
     * @throws IOException           If writing fails or the item already exists in the
     *                               destination directory.
     * @throws IllegalStateException If the destination is not a directory or destination is on a
     *                               different file system.
     */
    /* package */void move(FAT16LongNameEntry entry, UsbFile destination) throws IOException {
        if (!destination.isDirectory())
            throw new IllegalStateException("destination cannot be a file!");
        if (!(destination instanceof FatDirectory))
            throw new IllegalStateException("cannot move between different filesystems!");
        // TODO check if destination is really on the same physical device or
        // partition!

        FatDirectory destinationDir = (FatDirectory) destination;
        if (destinationDir.lfnMap.containsKey(entry.getName().toLowerCase(Locale.getDefault())))
            throw new IOException("item already exists in destination!");

        init();
        destinationDir.init();

        // now the actual magic happens!
        removeEntry(entry);
        destinationDir.addEntry(entry, entry.getActualEntry());

        write();
        destinationDir.write();
    }

    @Override
    public void delete() throws IOException {
        if (isRoot())
            throw new IllegalStateException("Root dir cannot be deleted!");

        init();
        UsbFile[] subElements = listFiles();

        for (UsbFile file : subElements) {
            file.delete();
        }

        parent.removeEntry(entry);
        parent.write();

        fat.free(entry.getStartCluster());
    }

    @Override
    public String toString() {
        return "FatDirectory{" +
                "blockDevice=" + blockDevice +
                ", fat=" + fat +
                ", bootSector=" + bootSector +
                ", entries=" + entries +
                ", lfnMap=" + lfnMap +
                ", shortNameMap=" + shortNameMap +
                ", parent=" + parent +
                ", entry=" + entry +
                ", volumeLabel='" + volumeLabel + '\'' +
                ", hasBeenInited=" + hasBeenInited +
                '}';
    }
}
