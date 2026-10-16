# Changelog

All notable changes to ojAlgo-commons-math3 will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

## [4.2.0] – 2026-10-16

Built with ojAlgo 57.5.0 and Apache Commons Math 3.6.1.

### Changed

- A `SolverACM.Configurator` is looked up through the model's `Optimisation.Environment` (e.g. registered with `ExpressionsBasedModel.setConfigurator(...)`) instead of `Optimisation.Options`, which no longer holds configurators as of ojAlgo 57.1.0.

### Fixed

- The published POM declares the ojAlgo dependency (it was missing from 4.0.1).
- The objective function value returned by the solver includes the objective's constant.

## [4.0.1] – 2025-01-21

Built with ojAlgo 55.1.1 and Apache Commons Math 3.6.1. The published POM is missing the ojAlgo dependency – declare it yourself.

### Changed

- `SolverACM.Configurator` is a class instead of a functional interface: override `newSimplexSolver(Options)` to create and configure the `SimplexSolver`.
- `SolverACM.Integration` is no longer public; `SolverACM.INTEGRATION` is declared as `ExpressionsBasedModel.Integration<SolverACM>`.

## [4.0.0] – 2022-09-28

Built with ojAlgo 52.0.0 and Apache Commons Math 3.6.1.

### Changed

- `SolverCommonsMath` is renamed `SolverACM`, and it and `LinearOptimizerEBM` moved from `org.ojalgo.commons.math3.optim.linear` to `org.ojalgo.optimisation.solver.acm`.
- Requires Java 11 (was Java 8).

## [3.2.1] – 2022-07-06

Built with ojAlgo 51.4.0 and Apache Commons Math 3.6.1.

Dependency updates only.

## [3.2.0] – 2022-05-16

Built with ojAlgo 51.3.0 and Apache Commons Math 3.6.1.

### Added

- `LinearOptimizerEBM`, a drop-in replacement for Commons Math's `SimplexSolver` (same API, different class name) that builds and solves an ojAlgo `ExpressionsBasedModel`.

## [3.1.0] – 2021-10-27

Built with ojAlgo 49.2.1 and Apache Commons Math 3.6.1.

Dependency updates only.

## [3.0.0] – 2021-03-24

Built with ojAlgo 48.4.1 and Apache Commons Math 3.6.1.

### Changed

- Versioned and released independently of the other ojAlgo-extensions modules, ending the 2.47.x scheme. The published POM has no `ojalgo-extensions` parent and declares the ojAlgo dependency itself.

## [2.47.3] – 2019-04-29

Built with ojAlgo 47.1.2 and Apache Commons Math 3.6.1.

### Added

- `IntegerDistributionWrapper` and `RealDistributionWrapper` wrap Commons Math `IntegerDistribution` and `RealDistribution` instances, turning them into ojAlgo `DiscreteDistribution` and `ContinuousDistribution` implementations.

### Changed

- Changes to be compatible with ojAlgo v47.1 (`RealMatrixWrapper` supplies its elements to a `TransformableRegion`, previously `ElementsConsumer`).

## [2.47.2] – 2018-12-19

Built with ojAlgo 47.0.0 and Apache Commons Math 3.6.1.

### Added

- `SolverCommonsMath` lets `ExpressionsBasedModel` use Commons Math's `SimplexSolver` for LP, via `SolverCommonsMath.INTEGRATION`. An optional `SolverCommonsMath.Configurator`, set in the model's `Optimisation.Options`, can configure the `SimplexSolver`.

### Changed

- New version scheme: 2 is the POM structure version, 47 the ojAlgo major version, and the last digit the ojAlgo-finance major version.
- Built for ojAlgo v47, where the `org.ojalgo.access` package became `org.ojalgo.structure`.

## [1.0.0] – 2017-09-27

Built with ojAlgo 44.0.0 and Apache Commons Math 3.6.1.

Initial release. `RealMatrixWrapper` turns a Commons Math `RealMatrix` into an ojAlgo `MatrixStore`, whose elements can be supplied directly to ojAlgo's matrix decompositions without intermediate copies, and `Access2DWrapper` turns any ojAlgo `Access2D` into a `RealMatrix`.
