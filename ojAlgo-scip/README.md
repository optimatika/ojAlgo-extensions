# ojAlgo-scip

ojAlgo integration with the [SCIP](https://github.com/scipopt/scip) solver using Java's Foreign Function & Memory API (Panama FFI).

SCIP is a solver for LP, QP, MILP, MIQP, QCQP, and MIQCQP problems.

## Native Library Installation (macOS)

```bash
brew install scip
```

To update to the latest version:

```bash
brew upgrade scip
```

The library is loaded from `$(brew --prefix)/opt/scip/lib/libscip.dylib`.

## Usage

```java
ExpressionsBasedModel model = new ExpressionsBasedModel();
// ... build model ...
model.addIntegration(SolverSCIP.INTEGRATION);
Optimisation.Result result = model.minimise();
```
