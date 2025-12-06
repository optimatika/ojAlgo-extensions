# Clarabel4j Integration Plan

> Legend: [x] done, [ ] pending, [~] partial/in progress
>
> See also: `.github/copilot-instructions.md` in the main ojAlgo repo for global coding and testing conventions.

## 1. Core QP integration

- [x] Map linear objective and bounds from `ExpressionsBasedModel` to Clarabel `q`, variable bounds, and linear constraints.
- [x] Map quadratic objective from `Expression` to Clarabel `P` so that Clarabel’s objective matches {@link ConvexSolver} on plain QPs.
  - [x] Verify on a small handcrafted QP (`SmallQPComparisonTest`).
  - [x] Verify on the Maros–Mészáros suite (`CuteMarosMeszarosCaseWithClarabel4j`) – currently 34/35 tests passing; `QPCBOEI1` remains numerically problematic.
- [x] Centralise EBM→Clarabel mapping in `SolverClarabel4j.Integration.buildClarabelSolver`.
- [x] Remove the temporary `ClarabelData` DTO and keep the mapping logic directly inside `SolverClarabel4j`.
- [x] Retain the internal `Triplet` helper and `buildCSCMatrix(...)` as the common path for building Clarabel CSC matrices for `P` and `A`.

## 2. Test coverage and parity with ojAlgo solvers

- [x] Reuse existing ojAlgo convex/QP tests by wiring Clarabel as an {@link ExpressionsBasedModel.Integration}.
  - [x] `CuteNetlibCaseWithClarabel4j` for LPs.
  - [x] `CuteMarosMeszarosCaseWithClarabel4j` for QPs.
  - [x] Simple convex problems (`ConvexProblemsWithClarabel4j`, `SmallQPComparisonTest`).
- [~] Align Clarabel’s behaviour with {@link ConvexSolver} and {@link SolverMosek} on all Maros–Mészáros instances.
  - [x] Fix objective scaling and mapping so that all objective mismatches except `QPCBOEI1` are resolved.
  - [ ] Investigate `QPCBOEI1` specifically: Clarabel currently returns a solution that `ModelFileTest.assertValues(...)` deems invalid for a minimisation.
    - [ ] Inspect KKT residuals and state for `QPCBOEI1` with additional diagnostics.
    - [ ] Decide whether to treat this as a Clarabel numerical limitation (document it) or tune options/tolerances to recover a valid solution.

## 3. Future cone and quadratic-constraint support

- [ ] Second-order cones and quadratic constraints
  - [ ] Implement `appendQuadraticConstraint(...)` in `SolverClarabel4j` to map supported quadratic constraints to Clarabel cones (e.g. `SecondOrderCone`).
  - [ ] Start with simple, well-understood patterns (norm/ellipsoid constraints) and use targeted tests rather than Maros–Mészáros.
  - [ ] Reuse and mirror patterns from {@link ConvexSolver} and any existing cone-based integrations (if present in ojAlgo or extensions).
- [ ] Additional cones (when needed by ojAlgo models)
  - [ ] `ZeroCone` and `NonnegativeCone` are already in use for equalities and inequalities.
  - [ ] Add support for other cones (`ExponentialCone`, `PowerCone`, `GenPowerCone`, `SecondOrderCone`, etc.) only when there are concrete EBM use-cases and tests.
  - [ ] Keep the mapping logic in `buildClarabelSolver` / `appendQuadraticConstraint` so that all EBM→Clarabel conversions remain in one place.

## 4. Robustness, diagnostics, and options

- [~] Diagnostics and logging
  - [x] Add a `Configurator.diagnostics(boolean)` hook on `SolverClarabel4j` to toggle extra model metadata logging via `ExpressionsBasedModel.describe()` when builds/solves fail.
  - [ ] Consider adding targeted logging around:
    - [ ] Number of variables/constraints and nonzeros in `P` and `A`.
    - [ ] Detected quadratic constraints and their patterns when `appendQuadraticConstraint` is implemented.
- [ ] Options and numerical robustness
  - [ ] Expose a small, curated subset of Clarabel options (tolerances, max iterations, scaling) via ojAlgo’s `Optimisation.Options` and `SolverClarabel4j.Configurator`.
  - [ ] Use the Maros–Mészáros and Netlib suites to tune defaults for robustness vs performance.
  - [ ] Document any instances where Clarabel’s behaviour intentionally diverges from ojAlgo’s interior-point solver (e.g. more conservative feasibility checks).

## 5. Work items tied to specific tests/cases

- [ ] `QPCBOEI1` (CuteMarosMeszarosCase)
  - [ ] Enable diagnostics and inspect Clarabel’s reported status, objective, and primal/dual residuals.
  - [ ] Compare Clarabel’s final solution and state against {@link ConvexSolver} and {@link SolverMosek} for this model.
  - [ ] Decide whether to:
    - Adjust Clarabel options (e.g. tolerances) for this class of problems,
    - Or document the limitation and possibly relax the test for Clarabel only (if justified).

## 6. Refactoring and cleanup

- [x] Inline `ClarabelData` into `SolverClarabel4j.Integration` to simplify the integration structure.
- [x] Keep `buildClarabelSolver(ExpressionsBasedModel)` as the single, testable mapping boundary from EBM to Clarabel data and model.
- [ ] If additional solvers in ojAlgo-extensions adopt a similar CSC triplet pattern, consider factoring out a shared CSC builder utility (while keeping binary compatibility).

---

This plan tracks the current Clarabel4j integration status and outlines the next incremental steps. Focus for the next iteration should be:

1. Deep dive on `QPCBOEI1` to understand and, if possible, resolve the remaining failure.
2. Design and implement a first, well-tested mapping for simple quadratic constraints into `SecondOrderCone` via `appendQuadraticConstraint(...)`.
3. Only then consider broader cone support and option-tuning based on real EBM workloads.