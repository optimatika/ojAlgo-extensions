# ojAlgo-rocksdb

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-rocksdb%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=86400)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-rocksdb/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-rocksdb</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [RocksDB](http://rocksdb.org). Provides a `java.util.Map` implementation backed by RocksDB.

The `org.rocksdb:rocksdbjni` JAR (pulled transitively) bundles the native libraries and loads them itself, so no separate installation is needed. It covers Linux (x86, x86-64, ARM64, ppc64le, s390x and RISC-V 64, each with glibc or musl), macOS (ARM64 and x86-64) and Windows (x86-64).

## Native access warning

On recent JDKs, loading the native library prints a warning about restricted methods. To silence it, add this JVM argument:

```
--enable-native-access=ALL-UNNAMED
```

## Smaller dependency

The default `rocksdbjni` JAR is large (~90 MB) because it contains the native libraries for all platforms. Some platforms also have a classifier JAR with the same Java classes and only that platform's native library: `linux64`, `linux64-musl`, `linux32`, `linux32-musl`, `osx` (ARM64 and x86-64) and `win64`. Linux on ARM64, ppc64le, s390x and RISC-V has none, and needs the default JAR.

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-rocksdb</artifactId>
    <version>X.Y.Z</version>
    <exclusions>
        <exclusion>
            <groupId>org.rocksdb</groupId>
            <artifactId>rocksdbjni</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.rocksdb</groupId>
    <artifactId>rocksdbjni</artifactId>
    <version>A.B.C</version> <!-- the RocksDB version ojalgo-rocksdb depends on -->
    <classifier>linux64</classifier>
</dependency>
```
