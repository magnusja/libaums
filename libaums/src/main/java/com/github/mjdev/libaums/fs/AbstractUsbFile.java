package com.github.mjdev.libaums.fs;

import android.util.Log;

import java.io.IOException;

/**
 * Created by magnusja on 3/1/17.
 */

public abstract class AbstractUsbFile implements UsbFile {
    private static final String TAG = AbstractUsbFile.class.getSimpleName();

    @Override
    public UsbFile search(String path) throws IOException {

        if(!isDirectory()) {
            throw new UnsupportedOperationException("This is a file!");
        }

        Log.d(TAG, "search file: " + path);

        if (isRoot() && path.equals(separator)) {
            return this;
        }

        if (isRoot() && path.startsWith(separator)) {
            path = path.substring(1);
        }
        if (path.endsWith(separator)) {
            path = path.substring(0, path.length() - 1);
        }

        int index = path.indexOf(UsbFile.separator);

        if(index < 0) {
            Log.d(TAG, "search entry: " + path);

            UsbFile file = searchThis(path);
            return file;
        } else {
            String subPath = path.substring(index + 1);
            String dirName = path.substring(0, index);
            Log.d(TAG, "search recursively " + subPath + " in " + dirName);

            UsbFile file = searchThis(dirName);
            if(file != null && file.isDirectory()) {
                Log.d(TAG, "found directory " + dirName);
                return file.search(subPath);
            }
        }

        Log.d(TAG, "not found " + path);

        return null;
    }

    private UsbFile searchThis(String name) throws IOException {
        for(UsbFile file: listFiles()) {
            if(file.getName().equals(name))
                return file;
        }

        return null;
    }

    @Override
    public String getAbsolutePath() {
        if (getParent().isRoot()) {
            return "/" + getName();
        }
        return getParent().getAbsolutePath() + UsbFile.separator + getName();
    }

    @Override
    public int hashCode() {
        return getAbsolutePath().hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        // TODO add getFileSystem and check if file system is the same
        // TODO check reference
        return obj instanceof UsbFile &&
                getAbsolutePath().equals(((UsbFile) obj).getAbsolutePath());
    }
}
