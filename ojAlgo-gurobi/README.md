# ojAlgo Gurobi integration

Use [Gurobi](http://www.gurobi.com) from within ojAlgo – use Gurobi as a solver from ExpressionsBasedModel.

When/if ojAlgo's built-in optimisation solvers are not capable of solving your model (fast enough) it is possible to plug in other solvers. Gurobi is one such solver where an integration already exists.

## Prerequisites

* Basic knowledge of how to use ojAlgo to model and solve optimisation problems
* Gurobi is installed and functional

## This is what you need to do

* Add this dependency to your project. Here's how to do that using maven:

```xml
<!-- https://mvnrepository.com/artifact/org.ojalgo/ojalgo-gurobi -->
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-gurobi</artifactId>
    <version>X.Y.Z</version>
</dependency>
```
* That POM declares properties that are paths to where the jar and native binaries are installed. You need to set these properties to match your installation:

```xml
<properties>
    <!-- You have to change this! -->
    <path.installation.gurobi>/Library/gurobi751</path.installation.gurobi>
    <path.jar.gurobi>${path.installation.gurobi}/mac64/lib/gurobi.jar</path.jar.gurobi>
    <path.native.gurobi>${path.installation.gurobi}/mac64/bin</path.native.gurobi>
</properties>
```
* When you run your program the JVM property 'java.library.path' must contain the path to the Gurobi binary. In my case the path is: '/Library/gurobi751/mac64/bin'

* To configure ExpressionsBasedModel to use Gurobi rather than ojAlgo's built-in solvers execute this line of code:

```java
ExpressionsBasedModel.addPreferredSolver(SolverGurobi.INTEGRATION);
```
* If you only want to use Gurobi when the built-in solvers cannot handle a particular model you should instead do this:

```java
ExpressionsBasedModel.addFallbackSolver(SolverGurobi.INTEGRATION);
```

