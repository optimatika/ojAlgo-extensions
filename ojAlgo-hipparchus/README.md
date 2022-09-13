# ojAlgo Hipparchus integration

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-hipparchus/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-hipparchus/)

```xml
<!-- https://mvnrepository.com/artifact/org.ojalgo/ojalgo-hipparchus -->
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-hipparchus</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

https://hipparchus.org

Interoperability between ojAlgo and Hipparchus

Hipparchus started as a fork of Apache Commons Math (ACM) 3.6.1. This integration module was created by copying the ACM integration module, and then fixing import statements and some minor code changes.

- Classes to convert between ojAlgo and Hipparchus matrix implementations. In particular Hipparchus' matrix instanes can have  their elements supplied directly to ojAlg's matrix decompositions internal storage without allocating intermediate memory. ojAlgo matrix decompositions typcally perform better.
- Use Hipparchus' `SimplexSolver` with ojAlgo's `ExpressionsBasedModel`. Read this before you do: https://www.ojalgo.org/2021/10/pure-java-lp-solver-benchmark/
- Drop-in replacement for Hipparchus' `SimplexSolver`...

## Use Hipparchus LP solver from ExpressionsBasedModel

* Add this dependency to your project.
* To configure ExpressionsBasedModel to use Commons Math rather than ojAlgo's built-in solvers execute this line of code:

```java
ExpressionsBasedModel.addIntegration(SolverHipparchus.INTEGRATION);
```

