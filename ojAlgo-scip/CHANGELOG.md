# Changelog

All notable changes to ojAlgo-scip will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Added

- A configured MIP gap (`Options.getConfiguredGapTolerance()`) is passed on to SCIP (`limits/gap` and `limits/absgap`).

### Fixed

- A solve stopped at a limit, or interrupted, while SCIP has a solution (e.g. a MIP incumbent) now returns that solution as `FEASIBLE`. Previously the state was `APPROXIMATE` and no solution was returned.
- The objective function value returned by the solver includes the objective's constant, and undoes ojAlgo's internal scaling of the objective.

## [1.1.0] – 2026-09-14

Built with ojAlgo 57.3.0 and SCIP 10.0.3 (installed separately).

### Added

- Support for `time_suffice` and `iterations_suffice`: SCIP first runs with the "suffice" limits (time and nodes), and if that did not produce a feasible solution it continues up to the "abort" limits.

## [1.0.0] – 2026-08-19

Built with ojAlgo 57.1.1 and SCIP 10.0.3 (installed separately).

Initial release. `SolverSCIP` lets `ExpressionsBasedModel` use SCIP for LP, QP, QCQP and their mixed-integer variants, calling its C library via Java's Foreign Function & Memory API – requires Java 25 and `--enable-native-access`, and SCIP installed on the system (e.g. `brew install scip` on macOS). It has time and node limits, parallelism, interruption, an optional concurrent solve, a customisable `Configurator` and is an updatable solver (variable bounds can be changed between solves).
