# ojAlgo-rocksdb

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-rocksdb%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-rocksdb/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-rocksdb</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [RocksDB](http://rocksdb.org). Provides a `java.util.Map` implementation backed by RocksDB.

The `rocksdbjni` Maven artifact (pulled transitively) bundles platform-specific native libraries, so no separate installation is needed.
