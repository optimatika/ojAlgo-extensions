# Changelog

All notable changes to ojAlgo-ssclp will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Added

- `SolverSSCLP` lets `ExpressionsBasedModel` use SSC-LP, a pure Java simplex and branch-and-bound solver, for LP and MIP models (no quadratic objectives or constraints).
- A customisable `Configurator` for SSC-LP's LP and MILP solver instances and its logging (turned off unless detailed logging is configured in the options). The iteration limit is passed on.
- SSC-LP (`org.ssclab:SSC-LP` 4.7.9) is a regular Maven Central dependency. The module itself is not published, as SSC-LP is GPLv3 licensed – build and install it locally.
