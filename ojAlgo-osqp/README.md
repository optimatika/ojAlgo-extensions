# ojAlgo-osqp

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-osqp</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with [OSQP](https://osqp.org) (Operator Splitting Quadratic Program) using Java's Foreign Function & Memory API (Panama FFI). Use OSQP as a solver from `ExpressionsBasedModel`.

## Requirements

**Java 25 or later.** The Foreign Function & Memory API is used to call the OSQP C library directly, and the module is compiled for release 25 — unlike ojAlgo itself, which targets Java 11.

Calling native code requires the access to be enabled, otherwise the JVM warns (and, in later releases, refuses):

```
--enable-native-access=ALL-UNNAMED
```

## Native Library Installation

The OSQP native library (`libosqp`) must be installed on your system.

### macOS

```bash
brew install osqp
```

The library is loaded from `$(brew --prefix)/lib/libosqp.dylib`.

### Linux

```bash
sudo apt install libosqp-dev
```

Or build from source — see the [OSQP documentation](https://osqp.org/docs/get_started/). The library is searched for at:

- `/usr/lib/libosqp.so`
- `/usr/local/lib/libosqp.so`
- `/usr/lib/x86_64-linux-gnu/libosqp.so`
- `/usr/lib/aarch64-linux-gnu/libosqp.so`

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverOSQP.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverOSQP.INTEGRATION);
```
