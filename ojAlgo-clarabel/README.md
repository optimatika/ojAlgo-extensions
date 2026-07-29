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

## Native Library Installation (macOS)

Clarabel does not publish pre-built binaries. The C library must be built from source using Rust.

### Prerequisites

- **Rust**: `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh`

### Install or update

Run the provided script to clone (or update), build, and install `libclarabel_c.dylib`:

```bash
./update-clarabel.sh
```

The script clones [Clarabel.cpp](https://github.com/oxfordcontrol/Clarabel.cpp) into a sibling directory if not already present, pulls the latest changes, builds the Rust wrapper, and copies the library to `/usr/local/lib/`.

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
