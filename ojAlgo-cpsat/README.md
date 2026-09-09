# ojAlgo-cpsat

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-cpsat</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [Google CP-SAT](https://developers.google.com/optimization/cp/cp_solver) using Java's Foreign Function & Memory API (Panama FFI). Use CP-SAT as a solver from `ExpressionsBasedModel`.

This module calls the OR-Tools C API directly via FFM — it does not use the `ortools-java` Maven artifact (that is what [ojAlgo-ortools](../ojAlgo-ortools/) does).

## Requirements

**Java 25 or later.** The Foreign Function & Memory API is used to call the OR-Tools C library directly, and the module is compiled for release 25 — unlike ojAlgo itself, which targets Java 11.

Calling native code requires the access to be enabled, otherwise the JVM warns (and, in later releases, refuses):

```
--enable-native-access=ALL-UNNAMED
```

## Native Library Installation

The OR-Tools native library (`libortools`) must be installed on your system.

### macOS

```bash
brew install or-tools
```

The library is loaded from `$(brew --prefix)/lib/libortools.dylib`.

### Linux

Install OR-Tools from source or from the [pre-built packages](https://developers.google.com/optimization/install). The library is searched for at:

- `/usr/lib/libortools.so`
- `/usr/local/lib/libortools.so`
- `/usr/lib/x86_64-linux-gnu/libortools.so`
- `/usr/lib/aarch64-linux-gnu/libortools.so`

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverCPSAT.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverCPSAT.INTEGRATION);
```
