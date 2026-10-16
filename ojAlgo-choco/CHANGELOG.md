# Changelog

All notable changes to ojAlgo-choco will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Changed

- Updated Choco Solver to 6.0.2.

### Fixed

- The reported objective function value is evaluated at the solution, in model units and including any constant term, rather than taken from Choco's (scaled and rounded) integer objective variable.
- Models with continuous variables: the search now decides the continuous variables and the objective variable as well, so that solutions are fully instantiated. Nogood recording from restarts is only used for pure integer models.

## [1.1.0] – 2026-09-21

Built with ojAlgo 57.3.1 and Choco Solver 6.0.1.

### Added

- Continuous variables are modelled as Choco real variables, with a precision that can be set by overriding `Configurator.precision()`. Constraints with fractional coefficients, or involving continuous variables, use Choco's mixed (real-valued) scalar constraints instead of being rounded to integers.
- Unbounded problems are detected and reported as `UNBOUNDED`.

### Changed

- Updated Choco Solver from 4.10.17 to 6.0.1. The module now requires Java 17.
- A fractional objective function is scaled (by at most 10 000) before Choco optimises its integer objective variable, instead of having its coefficients rounded.
- The default search is activity-based, with Luby restarts and nogood recording.

## [1.0.0] – 2026-09-14

Built with ojAlgo 57.3.0 and Choco Solver 4.10.17.

Initial release. Use the Choco Solver constraint programming solver, via `SolverChoco.INTEGRATION`, for linear models with at least one integer variable. All variables are modelled as integers, and coefficients, limits and bounds are rounded to `int`. Time limits are passed on, and a `SolverChoco.Configurator` can customise the Choco model.
