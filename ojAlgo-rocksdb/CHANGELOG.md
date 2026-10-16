# Changelog

All notable changes to ojAlgo-rocksdb will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

> Corresponds to changes in the `develop` branch since the last release

## [1.1.1] – 2026-10-16

Built with ojAlgo 57.5.0 and RocksDB 10.2.1.

### Changed

- Updated RocksDB to 10.2.1 (was 9.8.4).

### Fixed

- The published POM declares the ojAlgo dependency (it was missing from 1.1.0).

## [1.1.0] – 2025-01-24

Built with ojAlgo 55.1.1 and RocksDB 9.8.4. The published POM is missing the ojAlgo dependency – declare it yourself.

### Added

- `RocksMap.Builder.key(Function, Function)` and `value(Function, Function)`: specify how keys and values are converted to and from `byte[]` with a pair of functions, as an alternative to implementing a `Converter`.

## [1.0.0] – 2022-09-28

Built with ojAlgo 52.0.0 and RocksDB 7.6.0.

Initial release. `RocksMap` is a `java.util.Map` backed by RocksDB, created with `RocksMap.newBuilder(File)`: key and value `Converter`s, compression, block cache and max open files settings, optional sharding over several RocksDB instances, and a write-only bulk loading mode (`Mode.LOAD`). RocksDB is a regular Maven Central dependency (`org.rocksdb:rocksdbjni`, which bundles the native libraries).
