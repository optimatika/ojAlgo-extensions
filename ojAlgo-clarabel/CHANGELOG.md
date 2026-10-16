# Changelog

All notable changes to ojAlgo-clarabel will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Added

- Results include dual values and the reduced gradient, following the dual value convention introduced in ojAlgo 57.5.0.
- Infeasibility certificates: an `INFEASIBLE` result has Clarabel's certificate of primal infeasibility as its dual values, plus the certificate's reduced gradient.

### Changed

- An `UNBOUNDED` result comes with a feasible solution. When Clarabel reports dual infeasibility, the constraints are solved again with a zero objective, giving either a feasible solution (`UNBOUNDED`), an infeasibility certificate (`INFEASIBLE`), or `INVALID`.
- Clarabel's almost solved, iteration limit, time limit and interrupted statuses are `APPROXIMATE` with Clarabel's final iterate as the solution. Previously there was no solution, and the limits gave `FAILED`. The "almost" infeasible statuses are `INFEASIBLE` and `UNBOUNDED`.
- Clarabel's tolerances are no longer taken from `options.feasibility`, but fixed: 5E-9 for feasibility, and 5E-9 absolute / 1E-11 relative for the duality gap and infeasibility detection.
- The time limit is the smaller of `time_suffice` and `time_abort`.

### Fixed

- Fixed variables are passed as equality constraints, rather than as a pair of opposing inequalities that leave the interior point method no interior.
- Off-diagonal quadratic objective terms were not passed correctly to Clarabel, which expects the upper triangle of the Hessian.
- The objective function value returned by the solver includes the objective's constant, and undoes ojAlgo's internal scaling of the objective.

## [1.0.0] – 2026-08-19

Built with ojAlgo 57.1.1 and Clarabel 0.11.1 (built from source and installed separately).

Initial release. `SolverClarabel` lets `ExpressionsBasedModel` use the Clarabel interior point solver for LP, QP and convex QCQP (quadratic constraints become second-order cones; non-convex ones are rejected when the solver is built), calling its C library via Java's Foreign Function & Memory API – requires Java 25 and `--enable-native-access`. Clarabel's C library has to be built from source and installed separately (see `update-clarabel.sh`). It has iteration and time limits, parallelism, interruption and a customisable `Configurator`.
