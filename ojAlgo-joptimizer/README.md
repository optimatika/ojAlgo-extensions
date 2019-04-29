# ojAlgo JOptimizer integration

Use [JOptimizer](http://www.joptimizer.com) from within ojAlgo – use JOptimizer as a solver from ExpressionsBasedModel.

When/if ojAlgo's built-in optimisation solvers are not capable of solving your model (fast enough) it is possible to plug in other solvers. JOptimizer is one such solver where an integration already exists.

## Prerequisites

* Basic knowledge of how to use ojAlgo to model and solve optimisation problems

## This is what you need to do

* Add this dependency to your project. Here's how to do that using maven:

```xml
<!-- https://mvnrepository.com/artifact/org.ojalgo/ojalgo-joptimizer -->
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-joptimizer</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

* To configure ExpressionsBasedModel to use JOptimizer rather than ojAlgo's built-in solvers execute this line of code:

```java
ExpressionsBasedModel.addPreferredSolver(SolverJOptimizer.INTEGRATION);
```
* If you only want to use JOptimizer when the built-in solvers cannot handle a particular model you should instead do this:

```java
ExpressionsBasedModel.addFallbackSolver(SolverJOptimizer.INTEGRATION);
```
