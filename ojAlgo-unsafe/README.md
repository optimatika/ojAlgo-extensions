# ojAlgo sun.misc.Unsafe extensions

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-unsafe/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-unsafe/)

```xml
<!-- https://mvnrepository.com/artifact/org.ojalgo/ojalgo-unsafe -->
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-unsafe</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

sun.misc.Unsafe

Extend ojAgo with off-heap arrays (allocated using `sun.misc.Unsafe`). More precisely provides `org.ojalgo.array.DenseArray.Factory` instances that create off-heap memory based "array" instances that integrate with everything in ojAlgo's `org.ojalgo.array` package.

This blog post may shed some light on how to use them: https://www.ojalgo.org/2021/08/working-with-arrays/

ojAlgo-unsafe is available at [The Central (Maven) Repository](https://mvnrepository.com/artifact/org.ojalgo/ojalgo-unsafe) to be used with your favourite dependency management tool.


