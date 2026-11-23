# Changelog

All notable changes to ojAlgo-hipparchus will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Added

- `QPDualActiveSolverImpl`, for convex QP with Hipparchus' new `QPDualActiveSolver` – the dual active-set method of Goldfarb and Idnani. It solves accurately, provides dual values, and detects infeasibility. The model must be convex – Hipparchus regularises the Hessian until it can be factorised, so a non-convex model is not detected.
- `SQPOptimizerImplS2`, for convex QP and QCQP with Hipparchus' new `SQPOptimizerS2`, the replacement for `SQPOptimizerS` and `SQPOptimizerGM`. The optimiser may return the multipliers of the previous iterate rather than those of the solution, so they are used for dual values and the reduced gradient only if they satisfy the optimality conditions at the solution. With the Maros-Meszaros QP models that was the case for only 5 of 26 – the other results have no dual values. It has no infeasibility detection: a solution that does not satisfy the constraints gives a `FAILED` result. With Hipparchus' default convergence tolerance (1e-7) the constraints may be violated by up to its square root – quadratic constraints were satisfied to about 6 digits.
- Quadratic constraints, with `SQPOptimizerImplS2` and with `SolverHipparchus` (that uses it for them). They must be convex – one-sided, with a positive semidefinite quadratic form if the upper limit is set, or a negative semidefinite one if the lower limit is. A model with any other quadratic constraint is not handled: `isCapable(..)` is false, and a forced integration throws an `IllegalArgumentException`. The objective may be linear or quadratic.

### Changed

- Requires Java 22 (was Java 11). Development has moved to target the latest version of ojAlgo, which requires at least Java 22.
- Updated Hipparchus to 4.1 (was 4.0.3).
- `SolverHipparchus` solves convex QP with `QPDualActiveSolverImpl` (was `ADMMQPOptimizerImpl`). It is much more accurate (ADMM's tolerance is 1e-5) and faster, and an infeasible problem gives an `INFEASIBLE` result (rather than `FAILED` when the iteration limit is reached). With the Maros-Meszaros test set it solved 42 of 48 models – the other 6, each with 10 000 variables, did not finish within 5 minutes (the solver works with dense matrices).
- Hipparchus 4.1 fixed the scaling (equilibration) in its ADMM optimiser, so `ADMMQPOptimizerImpl` results can differ slightly.
- `LinearOptimizerEBM` solves with ojAlgo's `LinearSolver`, that since ojAlgo 57.6.0 can return values that differ in the last digit (0.9999999999999999 rather than 1.0). `SimplexSolverTest.testEpsilon`, that compared with zero tolerance, now allows 1e-7.
- `RelaxedMIPCaseWithHipparchus.testNeos3421095cinca`, inherited from ojAlgo's `RelaxedMIPCase` since 57.6.0, is disabled. The model has big-M rows with 1E+17 coefficients, and Hipparchus' `SimplexSolver` leaves the binaries at 0 (rather than about 1E-13), violating those rows by up to 67 000. The objective function value is correct.

### Deprecated

- `SQPOptimizerImplGM` and `SQPOptimizerImplS` – Hipparchus deprecated `SQPOptimizerGM` and `SQPOptimizerS`, and will remove them in 5.0. Use `QPDualActiveSolverImpl` or `SQPOptimizerImplS2` instead. They are also unreliable, and with an infeasible problem they may never return: they solve their QP sub-problems with an ADMM optimiser of their own, that has no iteration limit (and none can be set).

## [2.2.0] – 2026-10-16

Built with ojAlgo 57.5.0 and Hipparchus 4.0.3.

### Added

- Results from the QP optimisers (`ADMMQPOptimizerImpl`, `SQPOptimizerImplGM`, `SQPOptimizerImplS`, and `SolverHipparchus` for QP models) include dual values and the reduced gradient, following the convention introduced in ojAlgo 57.5.0. Hipparchus' `SimplexSolver` provides none.

### Changed

- Updated Hipparchus to 4.0.3 (was 3.1).

### Fixed

- The published POM declares the ojAlgo dependency (it was missing from 2.0.0).
- `ADMMQPOptimizerImpl`, and `SolverHipparchus` for QP, never returned with an infeasible problem – Hipparchus' ADMM has no infeasibility detection and was given no iteration limit. The iterations are now limited to `Options.iterations_abort`, or 10 000 if that is not set, and reaching the limit gives a `FAILED` result.
- Maximisation with the QP optimisers: Hipparchus' constrained optimisers ignore `GoalType`, so a maximisation was solved as a minimisation. The objective is now negated instead.
- `SQPOptimizerImplGM` and `SQPOptimizerImplS` ignored all inequality constraints, including variable bounds: they were passed as two-sided `LinearBoundedConstraint`s, which those optimisers do not read. They are now passed as one-sided `LinearInequalityConstraint`s, one per limit.
- The objective function value returned by the solver includes the objective's constant.
- Other failures inside Hipparchus' optimisers than infeasible/unbounded (e.g. a `NullPointerException` from the SQP optimisers) give a `FAILED` result rather than an exception.

## [2.0.0] – 2025-01-21

Built with ojAlgo 55.1.1 and Hipparchus 3.1. The published POM is missing the ojAlgo dependency – declare it yourself.

### Added

- Convex QP (quadratic objective, linear constraints) with Hipparchus' constrained optimisers: `ADMMQPOptimizerImpl`, `SQPOptimizerImplGM` and `SQPOptimizerImplS`, each with its own `INTEGRATION`.
- `SimplexSolverImpl.INTEGRATION`, for LP with Hipparchus' `SimplexSolver`.

### Changed

- `SolverHipparchus.INTEGRATION` handles both LP (with `SimplexSolver`) and convex QP (with `ADMMQPOptimizer`). It is declared as `ExpressionsBasedModel.Integration<SolverHipparchus>`, and `SolverHipparchus.Integration` is no longer public.

### Removed

- `SolverHipparchus.Configurator`. Each solver class has a nested `Configurator` instead.

## [1.0.0] – 2022-09-28

Built with ojAlgo 52.0.0 and Hipparchus 2.2.

Initial release, created from the Apache Commons Math integration (Hipparchus is a fork of Commons Math). `RealMatrixWrapper` and `Access2DWrapper` convert between Hipparchus and ojAlgo matrices, `RealDistributionWrapper` and `IntegerDistributionWrapper` turn Hipparchus distributions into ojAlgo ones, `SolverHipparchus` lets `ExpressionsBasedModel` use Hipparchus' `SimplexSolver` for LP (with an optional `Configurator`), and `LinearOptimizerEBM` is a drop-in replacement for Hipparchus' `SimplexSolver` that solves with ojAlgo.
