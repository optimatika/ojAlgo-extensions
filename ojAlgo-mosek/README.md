# ojAlgo Mosek integration

Use [Mosek](https://mosek.com) from within ojAlgo

## Prerequisites

* Basic knowledge of how to use ojAlgo to model and solve optimisation problems
* Mosek is installed and functional

## This is what you need to do

* The classpath must contain "this library", "ojAlgo" and the mosek.jar that is distributed with the Mosek software
* The jvm property 'java.library.path' must contain the path to the Mosek installation. In my case the path is: /Users/apete/Applications/mosek/7/tools/platform/osx64x86/bin
* Execute this one line of code:
```java
ExpressionsBasedModel.addIntegration(SolverMosek.INTEGRATION);
```
* Construct an [ExpressionsBasedModel](https://github.com/optimatika/ojAlgo/wiki/The-Diet-Problem) as you would normally using ojAlgo.
