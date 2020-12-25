/*
 * Copyright 1997-2020 Optimatika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ojalgo.optimisation.solver.mosek;

/**
 * A reimplementation of the example code case_portfolio_3.java supplied by Mosek with the software package.
 * <br>
 * Running the supplied example gives this output:
 *
 * <pre>
Computer
  Platform               : MACOSX/64-X86

Problem
  Name                   :
  Objective sense        : max
  Type                   : CONIC (conic optimization problem)
  Constraints            : 28
  Cones                  : 7
  Scalar variables       : 34
  Matrix variables       : 0
  Integer variables      : 0

Optimizer started.
Conic interior-point optimizer started.
Presolve started.
Linear dependency checker started.
Linear dependency checker terminated.
Eliminator - tries                  : 0                 time                   : 0.00
Eliminator - elim's                 : 0
Lin. dep.  - tries                  : 1                 time                   : 0.00
Lin. dep.  - number                 : 0
Presolve terminated. Time: 0.00
Optimizer  - threads                : 16
Optimizer  - solved problem         : the primal
Optimizer  - Constraints            : 12
Optimizer  - Cones                  : 7
Optimizer  - Scalar variables       : 27                conic                  : 22
Optimizer  - Semi-definite variables: 0                 scalarized             : 0
Factor     - setup time             : 0.00              dense det. time        : 0.00
Factor     - ML order time          : 0.00              GP order time          : 0.00
Factor     - nonzeros before factor : 38                after factor           : 38
Factor     - dense dim.             : 0                 flops                  : 5.52e+02
ITE PFEAS    DFEAS    GFEAS    PRSTATUS   POBJ              DOBJ              MU       TIME
0   2.0e+00  1.1e+00  9.1e-01  0.00e+00   1.810000000e-01   0.000000000e+00   1.0e+00  0.00
1   4.4e-01  2.3e-01  2.0e-01  1.49e+00   9.172708922e-02   3.004306798e-01   2.2e-01  0.00
2   8.5e-02  4.5e-02  3.8e-02  2.65e+00   7.424117116e-02   7.516787080e-02   4.2e-02  0.00
3   2.1e-02  1.1e-02  9.7e-03  6.71e-01   7.291687791e-02   7.567766663e-02   1.1e-02  0.01
4   2.7e-03  1.4e-03  1.3e-03  1.16e+00   7.365678213e-02   7.411858681e-02   1.4e-03  0.01
5   4.5e-04  2.4e-04  2.1e-04  1.30e+00   7.414550999e-02   7.423350793e-02   2.3e-04  0.01
6   6.4e-05  3.4e-05  2.9e-05  1.26e+00   7.436202419e-02   7.437325652e-02   3.2e-05  0.01
7   9.4e-06  5.0e-06  4.3e-06  1.05e+00   7.438655154e-02   7.438816353e-02   4.7e-06  0.01
8   7.3e-07  3.8e-07  3.3e-07  1.01e+00   7.439036857e-02   7.439049194e-02   3.6e-07  0.01
9   6.0e-08  3.2e-08  2.7e-08  1.00e+00   7.439065495e-02   7.439066514e-02   3.0e-08  0.01
Interior-point optimizer terminated. Time: 0.01.

Optimizer terminated. Time: 0.03

Interior-point solution summary
  Problem status  : PRIMAL_AND_DUAL_FEASIBLE
  Solution status : OPTIMAL
  Primal.  obj: 7.4390654948e-02    Viol.  con: 2e-07    var: 0e+00    cones: 2e-09
  Dual.    obj: 7.4390665143e-02    Viol.  con: 2e-08    var: 2e-08    cones: 0e+00
eglected return 7.439065e-02 for gamma 5.000000e-02
 * </pre>
 *
 * @author apete
 */
public final class CasePortfolio3 {

    public static void main(final String[] args) {
        // TODO Auto-generated method stub

    }

}
