# ojAlgo Apache Commons Math integration

Contains classes to convert between ojAlgo and Apache matrix implementations. In particular Apache's matrix instanes can have  their elements supplied directly to ojAlg's matrix decompositions internal storage without allocating intermediate memory. ojAlgo matrix decompositions are typcally perform better.


```xml
<!-- https://mvnrepository.com/artifact/org.ojalgo/ojalgo-commons-math3 -->
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-commons-math3</artifactId>
    <version>X.Y.Z</version>
</dependency>
```



## Use the Apache Commons Math LP solver from ojAlgo

### This is what you need to do

* Execute this one line of code:
```java
ExpressionsBasedModel.addIntegration(SolverCommonsMath.INTEGRATION);
```
* Construct an [ExpressionsBasedModel](https://github.com/optimatika/ojAlgo/wiki/The-Diet-Problem) as you would normally using ojAlgo.

## Changelog

### [Not  yet released]

#### Added

* IntegerDistributionWrapper & RealDistributionWrapper that wrap ACM IntegerDistribution & RealDistribution instances turning them into ojAlgo DiscreteDistribution & ContinuousDistribution implementations.

### v2.47.2: 2018-12-19
