# ojAlgo-extensions

This is the subscriber repository for [ojAlgo](https://www.ojalgo.org/) and the [Optimatika Subscription](https://optimatika.se/subscription/). It is where subscriber-only content lives: solver integrations, pre-release builds, migration guides, and — over time — formulation patterns, tuning notes, benchmarks and diagnostic tooling.

Access is granted automatically when you subscribe via [Polar](https://polar.sh/optimatika) and revoked on cancellation.

## Solver integrations

ojAlgo has zero dependencies, but through these extensions it integrates with third-party solvers and libraries. Each module depends on ojAlgo and typically one third-party library.

| Module | Version |
| --- | --- |
| [ojAlgo-clarabel](ojAlgo-clarabel) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-clarabel%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-clarabel/versions) |
| [ojAlgo-clarabel4j](ojAlgo-clarabel4j) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-clarabel4j%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-clarabel4j/versions) |
| [ojAlgo-commons-math3](ojAlgo-commons-math3) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-commons-math3%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-commons-math3/versions) |
| [ojAlgo-cplex](ojAlgo-cplex) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-cplex%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-cplex/versions) |
| [ojAlgo-gurobi](ojAlgo-gurobi) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-gurobi%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-gurobi/versions) |
| [ojAlgo-highs](ojAlgo-highs) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-highs%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-highs/versions) |
| [ojAlgo-hipparchus](ojAlgo-hipparchus) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-hipparchus%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-hipparchus/versions) |
| [ojAlgo-jfreechart](ojAlgo-jfreechart) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-jfreechart%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-jfreechart/versions) |
| [ojAlgo-joptimizer](ojAlgo-joptimizer) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-joptimizer%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-joptimizer/versions) |
| [ojAlgo-mosek](ojAlgo-mosek) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-mosek%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-mosek/versions) |
| [ojAlgo-ortools](ojAlgo-ortools) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-ortools%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-ortools/versions) |
| [ojAlgo-rocksdb](ojAlgo-rocksdb) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-rocksdb%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-rocksdb/versions) |
| [ojAlgo-scip](ojAlgo-scip) | [![Maven Central](https://img.shields.io/badge/dynamic/xml?label=Maven%20Central&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fojalgo%2Fojalgo-scip%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&color=blue&cacheSeconds=300)](https://central.sonatype.com/artifact/org.ojalgo/ojalgo-scip/versions) |
| [ojAlgo-ssclp](ojAlgo-ssclp) | |

Read the individual README files in the respective module directories for more info.

## New builds

New versions of the solver integrations are built on request. As a subscriber you have the right to request a build against the current version of any solver listed above. Pre-release builds and migration guides are available here before a public release ships.

## Support

Subscribers get direct support by email — questions about ojAlgo, the [Optimisation Service](https://optimatika.se/optimisation-service/), formulation, solver selection and these integrations. See the [subscription page](https://optimatika.se/subscription/) for what each tier includes.

Bug reports and issues with existing code: https://github.com/optimatika/ojAlgo-extensions/issues

General discussion: https://github.com/optimatika/ojAlgo-extensions/discussions or https://github.com/optimatika/ojAlgo/discussions

## License

MIT — see [LICENSE](LICENSE). The released artifacts are freely available on Maven Central. It is direct access to this repository — the source, pre-release builds and subscriber content — that is part of the subscription.
