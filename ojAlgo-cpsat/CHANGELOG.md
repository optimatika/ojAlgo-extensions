# Changelog

All notable changes to ojAlgo-cpsat will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Added

- A configured MIP gap (`Options.getConfiguredGapTolerance()`) is passed on to CP-SAT as relative and absolute gap limits. `SolverCPSAT.Parameters` has the corresponding `setRelativeGapLimit(...)` and `setAbsoluteGapLimit(...)`.

### Fixed

- The reported objective function value includes the objective's constant term.

## [1.1.0] – 2026-09-21

Built with ojAlgo 57.3.1 and OR-Tools 9.15 (CP-SAT, from the separately installed `libortools` native library).

### Changed

- Continuous variables are no longer rounded to integers. They are scaled to a decimal resolution deduced from the model (7 decimals when nothing else is implied), so mixed-integer models are solved at that resolution.

### Fixed

- Models with large (scaled) coefficients or unbounded variables were rejected by CP-SAT's 64-bit overflow check. Scales and artificial bounds are now reduced to stay within its limits.

## [1.0.0] – 2026-09-14

Built with ojAlgo 57.3.0 and OR-Tools 9.15 (CP-SAT, from the separately installed `libortools` native library).

Initial release. Use Google's CP-SAT solver, via `SolverCPSAT.INTEGRATION`, for linear models with integer variables. It calls the OR-Tools C API with Java's Foreign Function & Memory API, so it requires Java 25 (and `--enable-native-access`), but not the `ortools-java` artifact. Time and iteration (conflict) limits, parallelism and thread interruption are passed on, and a `SolverCPSAT.Configurator` can set CP-SAT parameters.
