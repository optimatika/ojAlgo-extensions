# ojAlgo Apache Commons Math integration

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-commons-math3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-commons-math3/)

```xml
<!-- https://mvnrepository.com/artifact/org.ojalgo/ojalgo-commons-math3 -->
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-commons-math3</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

Interoperability between ojAlgo and Apache Commons Math (ACM)

- Classes to convert between ojAlgo and ACM matrix implementations. In particular Apache's matrix instanes can have  their elements supplied directly to ojAlg's matrix decompositions internal storage without allocating intermediate memory. ojAlgo matrix decompositions typcally perform better.
- Use Apache's `SimplexSolver` with ojAlgo's `ExpressionsBasedModel`. Read this before you do: https://www.ojalgo.org/2021/10/pure-java-lp-solver-benchmark/
- Drop-in replacement for Apache's `SimplexSolver`...

## Use Commons Math LP solver from ExpressionsBasedModel

* Add this dependency to your project.
* To configure ExpressionsBasedModel to use Commons Math rather than ojAlgo's built-in solvers execute this line of code:

```java
ExpressionsBasedModel.addIntegration(SolverCommonsMath.INTEGRATION);
```

## Changelog

Added / Changed / Deprecated / Fixed / Removed / Security

### [Not  yet released]

- ...

### [3.2.0] - 2022-05-16

#### Added

- `LinearOptimizerEBM`, a drop-in replacement to `org.apache.commons.math3.optim.linear.SimplexSolver` and/or the deprecated `org.apache.commons.math3.optimization.linear.SimplexSolver`.

### [3.1.0] - 2021-10-27

### [3.0.0] - 2021-03-24

### [2.47.3] - 2019-04-29

#### Added

- `IntegerDistributionWrapper` & `RealDistributionWrapper` that wrap ACM `IntegerDistribution` & `RealDistribution` instances turning them into ojAlgo `DiscreteDistribution` & `ContinuousDistribution` implementations.

#### Changed

- Changes to be compatible with ojAlgo v47.1.2

### [2.47.2] - 2018-12-19

### [1.0.0] - 2017-09-27

- Initial release

