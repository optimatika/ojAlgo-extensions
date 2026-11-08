# ojAlgo-joptimizer

[![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-joptimizer%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=86400)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-joptimizer/versions)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-joptimizer</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [JOptimizer](http://www.joptimizer.com). Use JOptimizer as a solver from `ExpressionsBasedModel`.

The model must be convex. Non-convex quadratic objectives or constraints, as well as quadratic constraints that are two-sided (or equalities), are rejected with an `IllegalArgumentException`.

JOptimizer's LP solver confines every variable to [-99999, 99999] – infinite bounds are replaced by these limits, and larger bounds are clamped. If an LP solution ends up at such a limit, the problem is either unbounded or has its optimum beyond the limit, and the result is reported as `FEASIBLE` rather than `OPTIMAL`. For the same reason an LP that is infeasible within those limits is reported as `FAILED` rather than `INFEASIBLE`, unless all variables are bounded within them.

To get accurate solutions the integration uses tighter tolerances than JOptimizer's defaults. Some (harder) LPs fail with those, and an LP that fails is then solved once more with JOptimizer's default tolerances. That second attempt can be customised by overriding `SolverJOptimizer.Configurator.configureFallback(...)`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverJOptimizer.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverJOptimizer.INTEGRATION);
```
