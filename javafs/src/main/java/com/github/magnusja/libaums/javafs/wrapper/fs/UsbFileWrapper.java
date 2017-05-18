package com.github.magnusja.libaums.javafs.wrapper.fs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.mjdev.libaums.fs.AbstractUsbFile;
import com.github.mjdev.libaums.fs.UsbFile;

import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.FSEntryCreated;
import org.jnode.fs.FSEntryLastAccessed;
import org.jnode.fs.FSFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by magnusja on 01/03/17.
 */

public class UsbFileWrapper extends AbstractUsbFile {

    private static final String TAG = UsbFileWrapper.class.getSimpleName();

    private FSEntry entry;
    private FSDirectory dir;
    private FSFile file;

    public UsbFileWrapper(FSEntry entry) throws IOException {
        this.entry = entry;

        if(entry.isDirectory()) {
            dir = entry.getDirectory();
        } else {
            file = entry.getFile();
        }
    }

    public UsbFileWrapper(FSDirectory dir) {
        this.dir = dir;
    }

    @Override
    public boolean isDirectory() {
        return dir != null;
    }

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public void setName(String newName) throws IOException {
        entry.setName(newName);
    }

    @Override
    public long createdAt() {
        if(entry instanceof FSEntryCreated) {
            try {
                return ((FSEntryCreated) entry).getCreated();
            } catch (IOException e) {
                Log.e(TAG, "error getting last accessed", e);
                return -1;
            }
        }
        return -2;
    }

    @Override
    public long lastModified() {
        try {
            return entry.getLastModified();
        } catch (IOException e) {
            Log.e(TAG, "error getting last modified", e);
            return -1;
        }
    }

    @Override
    public long lastAccessed() {
        if(entry instanceof FSEntryLastAccessed) {
            try {
                return ((FSEntryLastAccessed) entry).getLastAccessed();
            } catch (IOException e) {
                Log.e(TAG, "error getting last accessed", e);
                return -1;
            }
        }
        return -2;
    }

    @Override
    public UsbFile getParent() {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String[] list() throws IOException {
        if(dir == null) {
            throw new UnsupportedOperationException("This is a file!");
        }

        List<String> list = new ArrayList<>();

        Iterator<? extends FSEntry> iterator = dir.iterator();

        while(iterator.hasNext()) {
            FSEntry entry = iterator.next();
            list.add(entry.getName());
        }

        String[] array = new String[list.size()];
        array = list.toArray(array);

        return array;
    }

    @Override
    public UsbFile[] listFiles() throws IOException {
        if(dir == null) {
            throw new UnsupportedOperationException("This is a file!");
        }

        List<UsbFile> list = new ArrayList<>();

        Iterator<? extends FSEntry> iterator = dir.iterator();

        while(iterator.hasNext()) {
            FSEntry entry = iterator.next();
            list.add(new UsbFileWrapper(entry));
        }

        UsbFile[] array = new UsbFile[list.size()];
        array = list.toArray(array);

        return array;
    }

    @Override
    public long getLength() {
        if(dir != null) {
            throw new UnsupportedOperationException("This is a directory!");
        }

        return file.getLength();
    }

    @Override
    public void setLength(long newLength) throws IOException {
        if(dir != null) {
            throw new UnsupportedOperationException("This is a directory!");
        }

        file.setLength(newLength);
    }

    @Override
    public void read(long offset, ByteBuffer destination) throws IOException {
        if(dir != null) {
            throw new UnsupportedOperationException("This is a directory!");
        }

        file.read(offset, destination);
    }

    @Override
    public void write(long offset, ByteBuffer source) throws IOException {
        if(dir != null) {
            throw new UnsupportedOperationException("This is a directory!");
        }

        file.write(offset, source);
    }

    @Override
    public void flush() throws IOException {
        if(dir != null) {
            throw new UnsupportedOperationException("This is a directory!");
        }

        file.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    @Override
    public UsbFile createDirectory(String name) throws IOException {
        if(dir == null) {
            throw new UnsupportedOperationException("This is a file!");
        }
        return new UsbFileWrapper(dir.addDirectory(name));
    }

    @Override
    public UsbFile createFile(String name) throws IOException {
        if(dir == null) {
            throw new UnsupportedOperationException("This is a file!");
        }

        return new UsbFileWrapper(dir.addFile(name));
    }

    @Override
    public void moveTo(UsbFile destination) throws IOException {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void delete() throws IOException {
        entry.getParent().remove(entry.getName());
    }

    @Override
    public boolean isRoot() {
        try {
            return entry.getId().equals(entry.getFileSystem().getRootEntry().getId());
        } catch (IOException e) {
            Log.e(TAG, "error checking id for determining root", e);
            return false;
        }
    }
}
