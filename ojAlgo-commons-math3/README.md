# ojAlgo Apache Commons Math integration

Contains classes to convert between ojAlgo and Apache matrix implementations. In particular Apache's matrix instanes can have  their elements supplied directly to ojAlg's matrix decompositions internal storage without allocating intermediate memory. ojAlgo matrix decompositions typcally perform better.


## Use Commons Math LP solver from ExpressionsBasedModel

* Add this dependency to your project. Here's how to do that using maven:

```xml
<!-- https://mvnrepository.com/artifact/org.ojalgo/ojalgo-commons-math3 -->
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-commons-math3</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

* To configure ExpressionsBasedModel to use Commons Math rather than ojAlgo's built-in solvers execute this line of code:

```java
ExpressionsBasedModel.addPreferredSolver(SolverCommonsMath.INTEGRATION);
```
* If you only want to use Commons Math when the built-in solvers cannot handle a particular model you should instead do this:

```java
ExpressionsBasedModel.addFallbackSolver(SolverCommonsMath.INTEGRATION);
```

## Changelog

### [Not  yet released]

* ...

### v2.47.3: 2019-04-29

#### Added

* IntegerDistributionWrapper & RealDistributionWrapper that wrap ACM IntegerDistribution & RealDistribution instances turning them into ojAlgo DiscreteDistribution & ContinuousDistribution implementations.

#### Changed

* Changes to be compatible with the ojAlgo v47.1.2

### v2.47.2: 2018-12-19

