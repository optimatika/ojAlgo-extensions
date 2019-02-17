# ojAlgo CPLEX integration

Use [CPLEX](https://www.ibm.com/products/ilog-cplex-optimization-studio) from within ojAlgo – use CPLEX as a solver from ExpressionsBasedModel.

When/if ojAlgo's built-in optimisation solvers are not capable of solving your model (fast enough) it is possible to plug in other solvers. CPLEX is one such solver where an integration already exists.

## Prerequisites

* Basic knowledge of how to use ojAlgo to model and solve optimisation problems
* CPLEX is installed and functional

## This is what you need to do

* Add this dependency to your project. Here's how to do that using maven:

```xml
<!-- https://mvnrepository.com/artifact/org.ojalgo/ojalgo-cplex -->
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-cplex</artifactId>
    <version>X.Y.Z</version>
</dependency>
```
* That POM declares properties that are paths to where the jar and native binaries are installed. You need to set these properties to match your installation:

```xml
<properties>
    <!-- You have to change this! -->
    <path.installation.cplex>/Applications/CPLEX_Studio_Community128/cplex</path.installation.cplex>
    <path.jar.cplex>${path.installation.cplex}/lib/cplex.jar</path.jar.cplex>
    <path.native.cplex>${path.installation.cplex}/bin/x86-64_osx</path.native.cplex>
</properties>
```
* When you run your program the JVM property 'java.library.path' must contain the path to the CPLEX binary. In my case the path is: '/Applications/CPLEX_Studio_Community128/cplex/bin/x86-64_osx'

* To configure ExpressionsBasedModel to use CPLEX rather than ojAlgo's built-in solvers execute this line of code:

```java
ExpressionsBasedModel.addPreferredSolver(SolverCPLEX.INTEGRATION);
```
* If you only want to use CPLEX when the built-in solvers cannot handle a particular model you should instead do this:

```java
ExpressionsBasedModel.addFallbackSolver(SolverCPLEX.INTEGRATION);
```

