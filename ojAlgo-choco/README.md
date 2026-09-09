# ojAlgo-choco

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-choco</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Choco-solver](https://choco-solver.org). Use Choco as a solver from `ExpressionsBasedModel`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverChoco.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverChoco.INTEGRATION);
```
