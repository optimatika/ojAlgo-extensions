# ojAlgo Apache Commons Math integration

Contains classes to convert between ojAlgo and Apache matrix implementations. In particular Apache's matrix instanes can have  their elements supplied directly to ojAlg's matrix decompositions internal storage with allocating intermediate memory. ojAlgo matrix decompositions are typcally better performant.

## ojAlgo JOptimizer integration

Use the Apache Commons Math LP solver from ojAlgo

### This is what you need to do

* Execute this one line of code:
```java
ExpressionsBasedModel.addIntegration(SolverJOptimizer.INTEGRATION);
```
* Construct an [ExpressionsBasedModel](https://github.com/optimatika/ojAlgo/wiki/The-Diet-Problem) as you would normally using ojAlgo.

