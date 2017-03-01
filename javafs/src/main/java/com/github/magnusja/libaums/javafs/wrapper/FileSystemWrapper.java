package com.github.magnusja.libaums.javafs.wrapper;

import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;

import org.jnode.driver.ApiNotFoundException;
import org.jnode.driver.block.FSBlockDeviceAPI;

import java.io.IOException;

/**
 * Created by magnusja on 3/1/17.
 */

public class FileSystemWrapper implements FileSystem {

    private org.jnode.fs.FileSystem wrappedFs;

    public FileSystemWrapper(org.jnode.fs.FileSystem wrappedFs) {
        this.wrappedFs = wrappedFs;
    }

    @Override
    public UsbFile getRootDirectory() {
        return null;
    }

    @Override
    public String getVolumeLabel() {
        try {
            return wrappedFs.getVolumeName();
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public long getCapacity() {
        try {
            return wrappedFs.getTotalSpace();
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public long getOccupiedSpace() {
        try {
            return wrappedFs.getTotalSpace() - wrappedFs.getFreeSpace();
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public long getFreeSpace() {
        try {
            return wrappedFs.getFreeSpace();
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public int getChunkSize() {
        try {
            return wrappedFs.getDevice().getAPI(FSBlockDeviceAPI.class).getSectorSize();
        } catch (IOException e) {
            return 4096;
        } catch (ApiNotFoundException e) {
            return 4096;
        }
    }
}
