package com.github.mjdev.libaums.fs;

/**
 * Created by Yuriy on 25.09.2016.
 */

public interface BootSector {

    short getBytesPerSector();

    short getSectorsPerCluster();

    short getReservedSectors();

    byte getFatCount();

    long getTotalNumberOfSectors();

    long getSectorsPerFat();

    long getRootDirStartCluster();

    short getFsInfoStartSector();

    boolean isFatMirrored();

    byte getValidFat();

    int getBytesPerCluster();

    long getFatOffset(int fatNumber);

    long getDataAreaOffset();

    String getVolumeLabel();

    long getDataClusterCount();

}
