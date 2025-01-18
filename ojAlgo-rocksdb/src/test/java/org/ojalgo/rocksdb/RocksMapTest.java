/*
 * Copyright 1997-2025 Optimatika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ojalgo.rocksdb;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ojalgo.TestUtils;
import org.ojalgo.rocksdb.RocksMap.Mode;

public class RocksMapTest {

    @TempDir
    public File tempDir;

    @Test
    public void testBulkLoadShardsSwitchModeAndGet() throws Exception {

        File dir = new File(tempDir, "RocksDB");

        try (RocksMap<String, String> map = RocksMap.newBuilder(dir).shards(4).mode(Mode.LOAD).build()) {

            map.put("key0", "value0");
            map.put("key1", "value1");
            map.put("key2", "value2");
            map.put("key3", "value3");
            map.put("key4", "value4");
            map.put("key5", "value5");
            map.put("key6", "value6");
            map.put("key7", "value7");
            map.put("key8", "value8");
            map.put("key9", "value9");

            map.switchMode(Mode.LOOKUP);

            TestUtils.assertEquals("value0", map.get("key0"));
            TestUtils.assertEquals("value1", map.get("key1"));
            TestUtils.assertEquals("value2", map.get("key2"));
            TestUtils.assertEquals("value3", map.get("key3"));
            TestUtils.assertEquals("value4", map.get("key4"));
            TestUtils.assertEquals("value5", map.get("key5"));
            TestUtils.assertEquals("value6", map.get("key6"));
            TestUtils.assertEquals("value7", map.get("key7"));
            TestUtils.assertEquals("value8", map.get("key8"));
            TestUtils.assertEquals("value9", map.get("key9"));
        }
    }

    @Test
    public void testBulkLoadShardsSwitchToMixed() throws Exception {

        File dir = new File(tempDir, "RocksDB");

        try (RocksMap<String, String> map = RocksMap.newBuilder(dir).shards(4).mode(Mode.LOAD).build()) {

            map.put("key0", "value0");
            map.put("key1", "value1");
            map.put("key2", "value2");
            map.put("key3", "value3");
            map.put("key4", "value4");
            map.put("key5", "value5");
            map.put("key6", "value6");
            map.put("key7", "value7");
            map.put("key8", "value8");
            map.put("key9", "value9");

            map.switchMode(Mode.MIXED);

            TestUtils.assertEquals("value0", map.get("key0"));
            TestUtils.assertEquals("value1", map.get("key1"));
            TestUtils.assertEquals("value2", map.get("key2"));
            TestUtils.assertEquals("value3", map.get("key3"));
            TestUtils.assertEquals("value4", map.get("key4"));
            TestUtils.assertEquals("value5", map.get("key5"));
            TestUtils.assertEquals("value6", map.get("key6"));
            TestUtils.assertEquals("value7", map.get("key7"));
            TestUtils.assertEquals("value8", map.get("key8"));
            TestUtils.assertEquals("value9", map.get("key9"));

            map.put("key10", "value10");
            map.put("key11", "value11");
            map.put("key12", "value12");
            map.put("key13", "value13");
            map.put("key14", "value14");
            map.put("key15", "value15");
            map.put("key16", "value16");
            map.put("key17", "value17");
            map.put("key18", "value18");
            map.put("key19", "value19");

            TestUtils.assertEquals("value10", map.get("key10"));
            TestUtils.assertEquals("value11", map.get("key11"));
            TestUtils.assertEquals("value12", map.get("key12"));
            TestUtils.assertEquals("value13", map.get("key13"));
            TestUtils.assertEquals("value14", map.get("key14"));
            TestUtils.assertEquals("value15", map.get("key15"));
            TestUtils.assertEquals("value16", map.get("key16"));
            TestUtils.assertEquals("value17", map.get("key17"));
            TestUtils.assertEquals("value18", map.get("key18"));
            TestUtils.assertEquals("value19", map.get("key19"));

        }
    }

    @Test
    public void testMostBasicPutGet() throws Exception {

        File dir = new File(tempDir, "RocksDB");

        try (RocksMap<String, String> map = RocksMap.newBuilder(dir).build()) {

            map.put("key", "value");

            TestUtils.assertEquals("value", map.get("key"));
        }
    }

    @Test
    public void testRemove() throws Exception {

        File dir = new File(tempDir, "RocksDB");

        try (RocksMap<String, String> map = RocksMap.newBuilder(dir).shards(2).build()) {

            map.put("key0", "value0");
            map.put("key1", "value1");
            map.put("key2", "value2");
            map.put("key3", "value3");
            map.put("key4", "value4");
            map.put("key5", "value5");
            map.put("key6", "value6");
            map.put("key7", "value7");
            map.put("key8", "value8");
            map.put("key9", "value9");

            TestUtils.assertEquals("value0", map.get("key0"));
            TestUtils.assertEquals("value1", map.get("key1"));
            TestUtils.assertEquals("value2", map.get("key2"));
            TestUtils.assertEquals("value3", map.get("key3"));
            TestUtils.assertEquals("value4", map.get("key4"));
            TestUtils.assertEquals("value5", map.get("key5"));
            TestUtils.assertEquals("value6", map.get("key6"));
            TestUtils.assertEquals("value7", map.get("key7"));
            TestUtils.assertEquals("value8", map.get("key8"));
            TestUtils.assertEquals("value9", map.get("key9"));

            TestUtils.assertTrue(map.containsKey("key5"));

            map.remove("key5");

            TestUtils.assertFalse(map.containsKey("key5"));

            TestUtils.assertTrue(map.get("key5") == null);
        }
    }

    @Test
    public void testReplaceAll() throws Exception {

        File dir = new File(tempDir, "RocksDB");

        try (RocksMap<String, String> map = RocksMap.newBuilder(dir).shards(8).build()) {

            map.put("key0", "value0");
            map.put("key1", "value1");
            map.put("key2", "value2");
            map.put("key3", "value3");
            map.put("key4", "value4");
            map.put("key5", "value5");
            map.put("key6", "value6");
            map.put("key7", "value7");
            map.put("key8", "value8");
            map.put("key9", "value9");

            TestUtils.assertEquals("value0", map.get("key0"));
            TestUtils.assertEquals("value1", map.get("key1"));
            TestUtils.assertEquals("value2", map.get("key2"));
            TestUtils.assertEquals("value3", map.get("key3"));
            TestUtils.assertEquals("value4", map.get("key4"));
            TestUtils.assertEquals("value5", map.get("key5"));
            TestUtils.assertEquals("value6", map.get("key6"));
            TestUtils.assertEquals("value7", map.get("key7"));
            TestUtils.assertEquals("value8", map.get("key8"));
            TestUtils.assertEquals("value9", map.get("key9"));

            map.replaceAll((k, v) -> v + "A");

            TestUtils.assertEquals("value0A", map.get("key0"));
            TestUtils.assertEquals("value1A", map.get("key1"));
            TestUtils.assertEquals("value2A", map.get("key2"));
            TestUtils.assertEquals("value3A", map.get("key3"));
            TestUtils.assertEquals("value4A", map.get("key4"));
            TestUtils.assertEquals("value5A", map.get("key5"));
            TestUtils.assertEquals("value6A", map.get("key6"));
            TestUtils.assertEquals("value7A", map.get("key7"));
            TestUtils.assertEquals("value8A", map.get("key8"));
            TestUtils.assertEquals("value9A", map.get("key9"));

            AtomicInteger counter = new AtomicInteger();
            map.forEach((k, v) -> counter.incrementAndGet());
            TestUtils.assertEquals(10, counter.intValue());
        }
    }

}
