# Changelog

All notable changes to ojAlgo-osqp will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Added

- Results include dual values, for the model's constraints and for variable bounds, following the convention introduced in ojAlgo 57.5.0.
- Infeasibility certificates: an `INFEASIBLE` result has OSQP's certificate of primal infeasibility as its dual values, plus the certificate's reduced gradient.

### Changed

- Only models with a quadratic objective are handled – `isCapable(..)` no longer accepts LP models.
- Termination tolerances are OSQP's defaults, except a relative tolerance of 2E-4. A configured feasibility tolerance (`Options.getConfiguredFeasibilityTolerance()`) sets the absolute and relative tolerances. Previously `options.feasibility` was always used, also for infeasibility detection.
- Only variables with limits get a bound row, and `updateRange(..)` returns `false` for a variable that had no limits when the solver was built.

### Fixed

- The reduced gradient is in model units and the model's optimisation sense. Previously it was OSQP's raw variable-bound multipliers.
- An error during the solve (e.g. a failed polish) gives a `FAILED` result. Previously the result could have OSQP's status, even `OPTIMAL`, with no solution.
- The objective function value returned by the solver includes the objective's constant, and undoes ojAlgo's internal scaling of the objective.

## [1.0.0] – 2026-09-14

Built with ojAlgo 57.3.0 and OSQP 1.0.0 (installed separately).

Initial release. `SolverOSQP` lets `ExpressionsBasedModel` use OSQP for LP and convex QP, calling its C library via Java's Foreign Function & Memory API – requires Java 25 and `--enable-native-access`, and OSQP installed on the system (e.g. `brew install osqp` on macOS). Polishing is on, iteration and time limits come from the model's options, results include the reduced gradient, and it is an updatable solver (variable bounds can be changed between solves, warm start, `updateSettings(..)`), with a customisable `Configurator`.
