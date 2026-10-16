# Changelog

All notable changes to ojAlgo-hipparchus will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

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
