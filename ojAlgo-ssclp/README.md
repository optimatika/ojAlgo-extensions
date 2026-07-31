# ojAlgo-ssclp

ojAlgo integration with [SSC-LP](https://www.ssclab.org/). Use SSC-LP as a solver from `ExpressionsBasedModel`.

## Not released – licensing

**There is no released artifact for this module, and there will not be one for as long as the licensing situation remains as it is.**

SSC-LP is distributed under the GNU General Public License, Version 3, with no linking/classpath exception. This module is MIT licensed, like the rest of ojAlgo, but it exists solely to link against SSC-LP. Publishing it to Maven Central would mean advertising an MIT artifact that transitively requires GPLv3 – imposing copyleft obligations on anyone who depends on it, most likely without them noticing. That is not something to hand out with an MIT badge on it.

The source is here, and it works. Build and install it yourself if you want to use it:

```bash
mvn -pl ojAlgo-ssclp install
```

Doing so is a decision about your own project's licensing. Private use – building it, running it, benchmarking with it – carries no GPL obligations. Distributing an application built on top of it does.

If SSC-LP ever gains a linking exception, or is offered under a weaker licence, this module can be released as-is.

## Usage

```java
Optimisation.Environment environment = Optimisation.newEnvironment();
environment.addIntegration(SolverSSCLP.INTEGRATION);
ExpressionsBasedModel model = environment.newModel();
```

or

```java
model.minimise(SolverSSCLP.INTEGRATION);
```
