# Changelog

All notable changes to ojAlgo-highs will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Added

- Results include dual values, HiGHS' row duals, following the convention introduced in ojAlgo 57.5.0.
- Infeasibility certificates: an `INFEASIBLE` result for a continuous model has HiGHS' dual ray as its dual values, plus the certificate's reduced gradient.
- A configured MIP gap (`Options.getConfiguredGapTolerance()`) is passed on to HiGHS (`mip_rel_gap` and `mip_abs_gap`).

### Fixed

- A solve stopped at a limit, or interrupted, while HiGHS has a feasible solution (e.g. a MIP incumbent) now returns that solution as `FEASIBLE`. Previously the state was `APPROXIMATE` and no solution was returned.
- The reduced gradient is in model units. It was off by ojAlgo's internal scaling of the objective.
- The objective function value returned by the solver includes the objective's constant, and undoes ojAlgo's internal scaling of the objective.

## [2.1.0] – 2026-09-14

Built with ojAlgo 57.3.0 and HiGHS 1.15.1 (installed separately).

### Added

- Support for `time_suffice` and `iterations_suffice`: HiGHS first runs with the "suffice" limits, and if that did not produce a feasible solution it continues up to the "abort" limits.

## [2.0.0] – 2026-08-19

Built with ojAlgo 57.1.1 and HiGHS 1.15.1 (installed separately).

### Added

- A solve can be stopped by interrupting the solving thread (via HiGHS callbacks, which need HiGHS 1.6 or later).
- Parallelism: with a configured parallelism of at least 4, and more than 1000 variables, HiGHS runs in parallel mode with up to 8 threads.

### Changed

- The HiGHS native library is no longer bundled with the jar. HiGHS must be installed on the system (e.g. `brew install highs` on macOS), and is looked for in the usual installation directories. If HiGHS is already loaded in the process (e.g. by OR-Tools) that copy is used.
- `SolverHiGHS.Configurator.configure(..)` has a new signature, `configure(ExpressionsBasedModel, MemorySegment, Arena)`, giving access to the model rather than only its options.

## [1.0.0] – 2026-06-21

Built with ojAlgo 57.0.0 and HiGHS 1.13.1 (native libraries bundled in the jar for macOS, Linux and Windows).

Initial release. `SolverHiGHS` lets `ExpressionsBasedModel` use HiGHS for LP, convex QP and MIP (not MIQP or quadratic constraints), calling its C library via Java's Foreign Function & Memory API – requires Java 25 and `--enable-native-access`. It has time and iteration limits, a customisable `Configurator`, the reduced gradient in results, and is an updatable solver (variable bounds can be changed between solves, also in bulk with `updateBounds(..)`).
