/*
 * Copyright 1997-2026 Optimatika
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.ojalgo.TestUtils;
import org.rocksdb.RocksDB;

/**
 * ojAlgo-rocksdb depends on the default rocksdbjni JAR, and relies on it containing the native libraries for
 * all platforms. Some releases did not: 10.4.2 had none, and 10.6.2 and 10.7.5 lacked the x86 musl ones
 * (re-released as 10.6.2.1 and 10.7.5.1). The other tests only show that the library for the build machine is
 * there. This one checks for all of them, so that upgrading to an incomplete release fails the build.
 */
public class NativeLibrariesTest {

    private static final String[] LIBRARIES = { "librocksdbjni-linux32.so", "librocksdbjni-linux32-musl.so", "librocksdbjni-linux64.so",
            "librocksdbjni-linux64-musl.so", "librocksdbjni-linux-aarch64.so", "librocksdbjni-linux-aarch64-musl.so", "librocksdbjni-linux-ppc64le.so",
            "librocksdbjni-linux-ppc64le-musl.so", "librocksdbjni-linux-riscv64.so", "librocksdbjni-linux-riscv64-musl.so", "librocksdbjni-linux-s390x.so",
            "librocksdbjni-linux-s390x-musl.so", "librocksdbjni-osx-arm64.jnilib", "librocksdbjni-osx-x86_64.jnilib", "librocksdbjni-win64.dll" };

    @Test
    public void testAllPlatformsBundled() {

        ClassLoader loader = RocksDB.class.getClassLoader();

        List<String> missing = new ArrayList<>();
        for (String library : LIBRARIES) {
            if (loader.getResource(library) == null) {
                missing.add(library);
            }
        }

        TestUtils.assertTrue("Missing from rocksdbjni: " + missing, missing.isEmpty());
    }

}
