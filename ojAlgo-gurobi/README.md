# ojAlgo Gurobi integration

Use [Gurobi](http://www.gurobi.com) from within ojAlgo

```xml
<!-- https://mvnrepository.com/artifact/org.ojalgo/ojalgo-gurobi -->
<dependency>
    <groupId>org.ojalgo</groupId>
    <artifactId>ojalgo-gurobi</artifactId>
    <version>X.Y.Z</version>
</dependency>
```



## Prerequisites

* Basic knowledge of how to use ojAlgo to model and solve optimisation problems
* Gurobi is installed and functional

## This is what you need to do

* The classpath must contain "this library", "ojAlgo" and the gurobi.jar that is distributed with the Gurobi software
* The jvm property 'java.library.path' must contain the path to the Gurobi installation. In my case the path is: ????
* Execute this one line of code:
```java
ExpressionsBasedModel.addIntegration(SolverGurobi.INTEGRATION);
```
* Construct an [ExpressionsBasedModel](https://github.com/optimatika/ojAlgo/wiki/The-Diet-Problem) as you would normally using ojAlgo.
