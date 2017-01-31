# ojAlgo CPLEX integration

Use [CPLEX](http://www-03.ibm.com/software/products/en/ibmilogcpleoptistud) from within ojAlgo

## Prerequisites

* Basic knowledge of how to use ojAlgo to model and solve optimisation problems
* CPLEX is installed and functional

## This is what you need to do

* The classpath must contain "this library", "ojAlgo" and the cplex.jar that IBM distributes together with CPLEX
* The jvm property 'java.library.path' must contain the path to the cplex installation. In my case the path is: /Users/apete/Applications/IBM/ILOG/CPLEX_Studio_Community1262/cplex/bin/x86-64_osx
* Execute this one line of code:
```java
ExpressionsBasedModel.addIntegration(SolverCPLEX.INTEGRATION);
```
* Construct an [ExpressionsBasedModel](https://github.com/optimatika/ojAlgo/wiki/The-Diet-Problem) as you would normally using ojAlgo.
