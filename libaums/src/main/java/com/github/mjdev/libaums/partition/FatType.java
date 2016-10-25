package com.github.mjdev.libaums.partition;

/**
 * Created by poudanen on 22.09.16.
 * enum for check a current FAT type
 */

public enum FatType {
    FAT12((1 << 12) - 16, 0xFFFL, 1.5f, "FAT12   ") { //NOI18N

        @Override
        public long readEntry(byte[] data, int index) {
            final int idx = (int) (index * 1.5);
            final int b1 = data[idx] & 0xFF;
            final int b2 = data[idx + 1] & 0xFF;
            final int v = (b2 << 8) | b1;

            if ((index % 2) == 0) {
                return v & 0xFFF;
            } else {
                return v >> 4;
            }
        }

        @Override
        public void writeEntry(byte[] data, int index, long entry) {
            final int idx = (int) (index * 1.5);

            if ((index % 2) == 0) {
                data[idx] = (byte) (entry & 0xFF);
                data[idx + 1] = (byte) ((entry >> 8) & 0x0F);
            } else {
                data[idx] |= (byte) ((entry & 0x0F) << 4);
                data[idx + 1] = (byte) ((entry >> 4) & 0xFF);
            }
        }
    },

    /**
     * Represents a 16-bit file allocation table.
     */
    FAT16((1 << 16) - 16, 0xFFFFL, 2.0f, "FAT16   ") { //NOI18N

        @Override
        public long readEntry(byte[] data, int index) {
            final int idx = index << 1;
            final int b1 = data[idx] & 0xFF;
            final int b2 = data[idx + 1] & 0xFF;
            return (b2 << 8) | b1;
        }

        @Override
        public void writeEntry(byte[] data, int index, long entry) {
            final int idx = index << 1;
            data[idx] = (byte) (entry & 0xFF);
            data[idx + 1] = (byte) ((entry >> 8) & 0xFF);
        }
    },

    /**
     * Represents a 32-bit file allocation table.
     */
    FAT32((1 << 28) - 16, 0xFFFFFFFFL, 4.0f, "FAT32   ") { //NOI18N

        @Override
        public long readEntry(byte[] data, int index) {
            final int idx = index * 4;
            final long l1 = data[idx] & 0xFF;
            final long l2 = data[idx + 1] & 0xFF;
            final long l3 = data[idx + 2] & 0xFF;
            final long l4 = data[idx + 3] & 0xFF;
            return (l4 << 24) | (l3 << 16) | (l2 << 8) | l1;
        }

        @Override
        public void writeEntry(byte[] data, int index, long entry) {
            final int idx = index << 2;
            data[idx] = (byte) (entry & 0xFF);
            data[idx + 1] = (byte) ((entry >> 8) & 0xFF);
            data[idx + 2] = (byte) ((entry >> 16) & 0xFF);
            data[idx + 3] = (byte) ((entry >> 24) & 0xFF);
        }
    };

    private final long minReservedEntry;
    private final long maxReservedEntry;
    private final long eofCluster;
    private final long eofMarker;
    private final long bitMask;
    private final int maxClusters;
    private final String label;
    private final float entrySize;

    private FatType(int maxClusters,
                    long bitMask, float entrySize, String label) {

        this.minReservedEntry = (0xFFFFFF0L & bitMask);
        this.maxReservedEntry = (0xFFFFFF6L & bitMask);
        this.eofCluster = (0xFFFFFF8L & bitMask);
        this.eofMarker = (0xFFFFFFFL & bitMask);
        this.entrySize = entrySize;
        this.label = label;
        this.maxClusters = maxClusters;
        this.bitMask = bitMask;
    }

    public abstract long readEntry(byte[] data, int index);

    public abstract void writeEntry(byte[] data, int index, long entry);

    /**
     * Returns the maximum number of clusters this file system can address.
     *
     * @return the maximum cluster count supported
     */
    public long maxClusters() {
        return this.maxClusters;
    }

    public String getLabel() {
        return this.label;
    }

    public boolean isReservedCluster(long entry) {
        return ((entry >= minReservedEntry) && (entry <= maxReservedEntry));
    }

    public boolean isEofCluster(long entry) {
        return (entry >= eofCluster);
    }

    public long getEofMarker() {
        return eofMarker;
    }

    public float getEntrySize() {
        return entrySize;
    }

    public long getBitMask() {
        return bitMask;
    }
}
