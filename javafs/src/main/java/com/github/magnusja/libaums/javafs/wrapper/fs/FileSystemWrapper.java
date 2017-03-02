package com.github.magnusja.libaums.javafs.wrapper.fs;

import android.util.Log;

import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;

import org.jnode.driver.ApiNotFoundException;
import org.jnode.driver.block.FSBlockDeviceAPI;

import java.io.IOException;

/**
 * Created by magnusja on 3/1/17.
 */

public class FileSystemWrapper implements FileSystem {

    private static final String TAG = FileSystemWrapper.class.getSimpleName();

    private org.jnode.fs.FileSystem wrappedFs;

    public FileSystemWrapper(org.jnode.fs.FileSystem wrappedFs) {
        this.wrappedFs = wrappedFs;
    }

    @Override
    public UsbFile getRootDirectory() {
        try {
            return new UsbFileWrapper(wrappedFs.getRootEntry());
        } catch (IOException e) {
            Log.e(TAG, "error getting root entry", e);
            return null;
        }
    }

    @Override
    public String getVolumeLabel() {
        try {
            return wrappedFs.getVolumeName();
        } catch (IOException e) {
            Log.e(TAG, "error getting volume label", e);
            return "";
        }
    }

    @Override
    public long getCapacity() {
        try {
            return wrappedFs.getTotalSpace();
        } catch (IOException e) {
            Log.e(TAG, "error getting capacity", e);
            return 0;
        }
    }

    @Override
    public long getOccupiedSpace() {
        try {
            return wrappedFs.getTotalSpace() - wrappedFs.getFreeSpace();
        } catch (IOException e) {
            Log.e(TAG, "error getting total - free space", e);
            return 0;
        }
    }

    @Override
    public long getFreeSpace() {
        try {
            return wrappedFs.getFreeSpace();
        } catch (IOException e) {
            Log.e(TAG, "error getting free space", e);
            return 0;
        }
    }

    @Override
    public int getChunkSize() {
        try {
            // TODO this is wrong
            return wrappedFs.getDevice().getAPI(FSBlockDeviceAPI.class).getSectorSize();
        } catch (IOException e) {
            Log.e(TAG, "error getting sector size", e);
            return 4096;
        } catch (ApiNotFoundException e) {
            Log.e(TAG, "api not found (this should not happen)", e);
            return 4096;
        }
    }
}
