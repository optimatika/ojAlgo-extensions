# ojAlgo JOptimizer integration

Use [JOptimizer](http://www.joptimizer.com) from within ojAlgo


```xml
<!-- https://mvnrepository.com/artifact/org.ojalgo/ojalgo-joptimizer -->
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-joptimizer</artifactId>
    <version>X.Y.Z</version>
</dependency>
```



## This is what you need to do

* Execute this one line of code:
```java
ExpressionsBasedModel.addIntegration(SolverJOptimizer.INTEGRATION);
```
* Construct an [ExpressionsBasedModel](https://github.com/optimatika/ojAlgo/wiki/The-Diet-Problem) as you would normally using ojAlgo.
