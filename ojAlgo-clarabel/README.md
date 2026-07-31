# ojAlgo-clarabel

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-clarabel/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ojalgo/ojalgo-clarabel/)

```xml
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-clarabel</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

ojAlgo integration with the [Clarabel](https://github.com/oxfordcontrol/Clarabel.rs) solver using Java's Foreign Function & Memory API (Panama FFI).

Clarabel is an interior-point solver for convex conic programs supporting LP, QP, and QCQP.

Quadratic constraints are translated into second-order cones, which requires them to be convex. A constraint whose quadratic form is indefinite, or one with limits on both sides, describes a non-convex region and is rejected when the solver is built.

## Requirements

**Java 25 or later.** The Foreign Function & Memory API is used to call the Clarabel C library directly, and the module is compiled for release 25 — unlike ojAlgo itself, which targets Java 11.

Calling native code requires the access to be enabled, otherwise the JVM warns (and, in later releases, refuses):

```
--enable-native-access=ALL-UNNAMED
```

## Native Library Installation (macOS)

Clarabel does not publish pre-built binaries. The C library must be built from source using Rust.

### Prerequisites

- **Rust**: `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh`

### Install or update

Run the provided script to clone (or update), build, and install `libclarabel_c.dylib`:

```bash
./update-clarabel.sh
```

The script clones [Clarabel.cpp](https://github.com/oxfordcontrol/Clarabel.cpp) next to the ojAlgo-extensions repository (not inside it) if not already present, pulls the latest changes, builds the Rust wrapper, and copies the library to `/usr/local/lib/` — the last step needs `sudo`.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverClarabel.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverClarabel.INTEGRATION);
```
