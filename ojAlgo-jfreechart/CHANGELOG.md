# Changelog

All notable changes to ojAlgo-jfreechart will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

### Changed

- Requires Java 11 (was Java 8).
- Updated JFreeChart to 1.5.6.

## [1.0.0] – 2022-07-06

Built with ojAlgo 51.4.0 and JFreeChart 1.5.3.

Initial release, of code that has been part of ojAlgo-extensions since 2017. Builds JFreeChart charts directly from ojAlgo data structures: builders for bar, stacked bar, pie, scatter and stacked XY area charts, time series charts (`CalendarDateSeriesCollection`), history and forecast charts, and Monte Carlo portfolio simulations (`MonteCarloBuilder`), in the package `ext.ojalgo.jfree`. The resulting chart is wrapped in a `JFreeChartAdaptor`. Java 8 baseline.
