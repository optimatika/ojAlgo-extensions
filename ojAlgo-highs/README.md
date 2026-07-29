# ojAlgo-highs

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-highs/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-highs/)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-highs</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with the [HiGHS](https://github.com/ERGO-Code/HiGHS) solver using Java's Foreign Function & Memory API (Panama FFI).

HiGHS is a high-performance solver for LP, QP, and MILP problems.

## Native Library Installation (macOS)

```bash
brew install highs
```

To update to the latest version:

```bash
brew upgrade highs
```

The library is loaded from `$(brew --prefix)/opt/highs/lib/libhighs.dylib`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverHiGHS.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverHiGHS.INTEGRATION);
```
