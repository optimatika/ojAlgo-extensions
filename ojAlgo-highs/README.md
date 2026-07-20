# ojAlgo-highs

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
ExpressionsBasedModel model = new ExpressionsBasedModel();
// ... build model ...
model.addIntegration(SolverHiGHS.INTEGRATION);
Optimisation.Result result = model.minimise();
```
