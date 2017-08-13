package com.github.mjdev.libaums.partition;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.github.mjdev.libaums.driver.file.FileBlockDeviceDriver;
import com.github.mjdev.libaums.partition.mbr.MasterBootRecord;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by magnusja on 01/08/17.
 */
public class MasterBootRecordTest {

    private static Map<URL, URL> urls = new HashMap<>();
    private Map<URL, List<Map<String, Integer>>> tests = new HashMap<>();

    static {
        try {
            urls.put(new URL("https://www.dropbox.com/s/w3x12zw6d6lc6x5/mbr_1_partition_hfs%2B.bin?dl=1"),
                    new URL("https://www.dropbox.com/s/skvo5ffki2s5xem/mbr_1_partition_hfs%2B.json?dl=1"));
            urls.put(new URL("https://www.dropbox.com/s/qx5eutm1ysbw1gi/mbr_2_partition_fat32_ext4.bin?dl=1"),
                    new URL("https://www.dropbox.com/s/gdw2ai67bhnn9df/mbr_2_partition_fat32_ext4.json?dl=1"));
            urls.put(new URL("https://www.dropbox.com/s/tfpqywg9kvnfrp8/mbr_3_partition_fat32_ext4_fat32.bin?dl=1"),
                    new URL("https://www.dropbox.com/s/jsux53wok2d6894/mbr_3_partition_fat32_ext4_fat32.json?dl=1"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


    @Before
    public void setUp() throws Exception {
        for (Map.Entry<URL, URL> entry : urls.entrySet()) {
            URL mbrUrl = entry.getKey();
            URL infoUrl = entry.getValue();

            tests.put(mbrUrl, getTableInfo(infoUrl));
        }
    }

    private List<Map<String, Integer>> getTableInfo(URL url) throws IOException {
        JsonArray array = Json.parse(IOUtils.toString(url.openStream())).asArray();
        List<Map<String, Integer>> list = new ArrayList<>();

        for (JsonValue jsonValue : array) {
            Map<String, Integer> map = new HashMap<>();
            for (JsonObject.Member member : jsonValue.asObject()) {
                String name = member.getName();
                int value = member.getValue().asInt();
                map.put(name, value);
            }
            list.add(map);
        }

        return list;
    }

    @Test
    public void testRead() throws Exception {
        for (Map.Entry<URL, List<Map<String, Integer>>> entry : tests.entrySet()) {
            URL mbrUrl = entry.getKey();
            List<Map<String, Integer>> partitionTableInfo = entry.getValue();

            FileBlockDeviceDriver blockDevice = new FileBlockDeviceDriver(mbrUrl);
            blockDevice.init();

            ByteBuffer buffer = ByteBuffer.allocate(512);
            blockDevice.read(0, buffer);
            buffer.flip();
            MasterBootRecord mbr = MasterBootRecord.read(buffer);

            List<PartitionTableEntry> table = mbr.getPartitionTableEntries();
            Assert.assertEquals(mbrUrl.getFile(), partitionTableInfo.size(), table.size());

            int i = 0;
            for (PartitionTableEntry e : table) {
                Assert.assertEquals(mbrUrl.getFile(), partitionTableInfo.get(i).get("partitionType").intValue(),
                        e.getPartitionType());
                Assert.assertEquals(mbrUrl.getFile(), partitionTableInfo.get(i).get("logicalBlockAddress").intValue(),
                        e.getLogicalBlockAddress());
                Assert.assertEquals(mbrUrl.getFile(), partitionTableInfo.get(i).get("totalNumberOfSectors").intValue(),
                        e.getTotalNumberOfSectors());
                i++;
            }
        }
    }

    @Test(expected = IOException.class)
    public void testSizeMismatch() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(3);
        MasterBootRecord.read(buffer);
    }

    @Test
    public void testEmptyBuffer() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        Assert.assertNull(MasterBootRecord.read(buffer));
    }

}