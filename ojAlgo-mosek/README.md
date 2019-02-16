# ojAlgo Mosek integration

Use [Mosek](https://www.mosek.com) from within ojAlgo – use Mosek as a solver from ExpressionsBasedModel.

When/if ojAlgo's built-in optimisation solvers are not capable of solving your model (fast enough) it is possible to plug in other solvers. Mosek is one such solver where an integration already exists.

## Prerequisites

* Basic knowledge of how to use ojAlgo to model and solve optimisation problems
* Mosek is installed and functional

## This is what you need to do

* Add this dependency to your project. Here's how to do that using maven:

```xml
<!-- https://mvnrepository.com/artifact/org.ojalgo/ojalgo-mosek -->
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-mosek</artifactId>
    <version>X.Y.Z</version>
</dependency>
```
* That POM declares properties that are paths to where the jar and native binaries are installed. You need to set these properties to match your installation:

```xml
<properties>
    <!-- You have to change this! -->
    <path.installation.mosek>/Users/apete/Applications/mosek</path.installation.mosek>
    <path.jar.mosek>${path.installation.mosek}/7/tools/platform/osx64x86/bin/mosek.jar</path.jar.mosek>
    <path.native.mosek>${path.installation.mosek}/7/tools/platform/osx64x86/bin</path.native.mosek>
</properties>
```
* When you run your program the JVM property 'java.library.path' must contain the path to the Mosek binary. In my case the path is: '/Users/apete/Applications/mosek/7/tools/platform/osx64x86/bin'

* To configure ExpressionsBasedModel to use Mosek rather than ojAlgo's built-in solvers execute this line of code:

```java
ExpressionsBasedModel.addPreferredSolver(SolverMosek.INTEGRATION);
```
* If you only want to use Mosek when the built-in solvers cannot handle a particular model you should instead do this:

```java
ExpressionsBasedModel.addFallbackSolver(SolverMosek.INTEGRATION);
```

