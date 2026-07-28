/*
 * Copyright 1997-2026 Optimatika
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
package org.ojalgo.optimisation.solver.scip;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.UpdatableSolver;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;

/**
 * SCIP solver integration for ojAlgo.
 * <p>
 * SCIP (Solving Constraint Integer Programs) is one of the fastest non-commercial solvers for mixed-integer
 * programming (MIP) and mixed-integer nonlinear programming (MINLP). This integration uses Java's Foreign
 * Function &amp; Memory API (FFM) to directly call the SCIP C library.
 *
 * @see <a href="https://www.scipopt.org">SCIP Optimization Suite</a>
 */
public final class SolverSCIP implements Optimisation.Solver, UpdatableSolver {

    /**
     * Configuration options for the SCIP solver.
     * <p>
     * Subclass this to customize SCIP-specific settings.
     */
    public static class Configurator {

        protected void configure(final MemorySegment scip, final Arena arena, final Optimisation.Options options) {

            MemorySegment verbLevel = SCIPBindings.allocateString(arena, "display/verblevel");
            boolean verbose = options.logger_appender != null && options.logger_detailed;
            SCIPBindings.setIntParam(scip, verbLevel, verbose ? 4 : 0);

            if (options.time_abort > 0L) {
                MemorySegment timeLimit = SCIPBindings.allocateString(arena, "limits/time");
                double seconds = options.time_abort / 1000.0;
                SCIPBindings.setRealParam(scip, timeLimit, seconds);
            }

            if (options.iterations_abort > 0L) {
                MemorySegment nodeLimit = SCIPBindings.allocateString(arena, "limits/nodes");
                SCIPBindings.setLongintParam(scip, nodeLimit, options.iterations_abort);
            }
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverSCIP> {

        Integration() {
            super();
        }

        @Override
        public SolverSCIP build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            int sense = model.getOptimisationSense() == Optimisation.Sense.MAX ? SCIPBindings.OBJSENSE_MAXIMIZE : SCIPBindings.OBJSENSE_MINIMIZE;

            List<Variable> variables = model.getVariables();
            List<Expression> constraints = model.constraints().toList();
            Expression objective = model.objective();

            int n = variables.size();
            int m = constraints.size();

            Arena arena = Arena.ofConfined();

            try {

                // Create SCIP instance
                MemorySegment scipPtr = arena.allocate(ValueLayout.ADDRESS);
                SCIPBindings.create(scipPtr);
                MemorySegment scip = scipPtr.get(ValueLayout.ADDRESS, 0);

                SCIPBindings.includeDefaultPlugins(scip);

                MemorySegment probName = SCIPBindings.allocateString(arena, "ojAlgo");
                SCIPBindings.createProbBasic(scip, probName);

                SCIPBindings.setObjsense(scip, sense);

                configurator.configure(scip, arena, model.options);

                // Create variables
                MemorySegment[] varPtrs = new MemorySegment[n];

                for (int v = 0; v < n; v++) {
                    Variable variable = variables.get(v);

                    double lb = variable.getLowerLimit(false, NEGATIVE_INFINITY);
                    double ub = variable.getUpperLimit(false, POSITIVE_INFINITY);

                    double obj = ZERO;
                    IntIndex key = new IntIndex(v);
                    if (objective.getLinearKeySet().contains(key)) {
                        obj = objective.doubleValue(key, true);
                    }

                    int vartype;
                    if (variable.isBinary()) {
                        vartype = SCIPBindings.VARTYPE_BINARY;
                    } else if (variable.isInteger()) {
                        vartype = SCIPBindings.VARTYPE_INTEGER;
                    } else {
                        vartype = SCIPBindings.VARTYPE_CONTINUOUS;
                    }

                    MemorySegment varPtr = arena.allocate(ValueLayout.ADDRESS);
                    MemorySegment varName = SCIPBindings.allocateString(arena, variable.getName());

                    SCIPBindings.createVarBasic(scip, varPtr, varName, lb, ub, obj, vartype);

                    MemorySegment var = varPtr.get(ValueLayout.ADDRESS, 0);
                    SCIPBindings.addVar(scip, var);

                    varPtrs[v] = var;

                    SCIPBindings.releaseVar(scip, varPtr);
                }

                // Create constraints
                for (int c = 0; c < m; c++) {
                    Expression constraint = constraints.get(c);

                    double lhs = constraint.getLowerLimit(true, NEGATIVE_INFINITY);
                    double rhs = constraint.getUpperLimit(true, POSITIVE_INFINITY);

                    MemorySegment consPtr = arena.allocate(ValueLayout.ADDRESS);
                    MemorySegment consName = SCIPBindings.allocateString(arena, "c" + c);

                    if (constraint.isAnyQuadraticFactorNonZero()) {
                        SolverSCIP.createQuadraticConstraint(scip, arena, consPtr, consName, constraint, varPtrs, lhs, rhs);
                    } else {
                        SolverSCIP.createLinearConstraint(scip, arena, consPtr, consName, constraint, varPtrs, lhs, rhs);
                    }

                    MemorySegment cons = consPtr.get(ValueLayout.ADDRESS, 0);
                    SCIPBindings.addCons(scip, cons);
                    SCIPBindings.releaseCons(scip, consPtr);
                }

                // Quadratic objective terms
                if (objective.isAnyQuadraticFactorNonZero()) {
                    SolverSCIP.addQuadraticObjective(scip, arena, objective, varPtrs, sense);
                }

                return new SolverSCIP(scip, varPtrs, n, arena);

            } catch (RuntimeException e) {
                arena.close();
                throw e;
            }
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return true;
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverSCIP> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    private static void addQuadraticObjective(final MemorySegment scip, final Arena arena, final Expression objective, final MemorySegment[] varPtrs,
            final int sense) {

        // Add an auxiliary variable for the quadratic part: obj_aux with objective coefficient 1
        MemorySegment auxVarPtr = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment auxVarName = SCIPBindings.allocateString(arena, "_qobj_");
        SCIPBindings.createVarBasic(scip, auxVarPtr, auxVarName, NEGATIVE_INFINITY, POSITIVE_INFINITY, ONE, SCIPBindings.VARTYPE_CONTINUOUS);
        MemorySegment auxVar = auxVarPtr.get(ValueLayout.ADDRESS, 0);
        SCIPBindings.addVar(scip, auxVar);
        SCIPBindings.releaseVar(scip, auxVarPtr);

        // Build quadratic constraint: obj_aux = x'Qx (with adjusted coefficients)
        // As: x'Qx - obj_aux = 0, i.e. lhs = 0, rhs = 0

        // Linear part: -1 * obj_aux (no linear terms from the quadratic part of the objective)
        MemorySegment[] linVars = new MemorySegment[]{ auxVar };
        double[] linCoefs = new double[]{ -ONE };

        boolean maximise = sense == SCIPBindings.OBJSENSE_MAXIMIZE;

        var quadKeys = objective.getQuadraticKeySet();
        List<MemorySegment> qv1 = new ArrayList<>();
        List<MemorySegment> qv2 = new ArrayList<>();
        List<Double> qc = new ArrayList<>();

        for (IntRowColumn key : quadKeys) {
            double val = objective.doubleValue(key, true);
            if (val != ZERO) {
                qv1.add(varPtrs[key.row]);
                qv2.add(varPtrs[key.column]);
                qc.add(maximise ? -val : val);
            }
        }

        int nquad = qv1.size();
        MemorySegment qv1Seg = SCIPBindings.allocatePointerArray(arena, qv1.toArray(new MemorySegment[0]));
        MemorySegment qv2Seg = SCIPBindings.allocatePointerArray(arena, qv2.toArray(new MemorySegment[0]));
        double[] qcArr = new double[nquad];
        for (int i = 0; i < nquad; i++) {
            qcArr[i] = qc.get(i);
        }
        MemorySegment qcSeg = SCIPBindings.allocateDoubleArray(arena, qcArr);

        MemorySegment linVarsSeg = SCIPBindings.allocatePointerArray(arena, linVars);
        MemorySegment linCoefsSeg = SCIPBindings.allocateDoubleArray(arena, linCoefs);

        MemorySegment consPtr = arena.allocate(ValueLayout.ADDRESS);
        MemorySegment consName = SCIPBindings.allocateString(arena, "_qobj_cons_");
        SCIPBindings.createConsBasicQuadraticNonlinear(scip, consPtr, consName, 1, linVarsSeg, linCoefsSeg, nquad, qv1Seg, qv2Seg, qcSeg, ZERO, ZERO);

        MemorySegment cons = consPtr.get(ValueLayout.ADDRESS, 0);
        SCIPBindings.addCons(scip, cons);
        SCIPBindings.releaseCons(scip, consPtr);
    }

    private static void createLinearConstraint(final MemorySegment scip, final Arena arena, final MemorySegment consPtr, final MemorySegment consName,
            final Expression constraint, final MemorySegment[] varPtrs, final double lhs, final double rhs) {

        var linearKeys = constraint.getLinearKeySet();
        int nvars = linearKeys.size();

        MemorySegment[] consVarPtrs = new MemorySegment[nvars];
        double[] consVals = new double[nvars];

        int idx = 0;
        for (IntIndex ckey : linearKeys) {
            consVarPtrs[idx] = varPtrs[ckey.index];
            consVals[idx] = constraint.doubleValue(ckey, true);
            idx++;
        }

        MemorySegment consVarsSeg = SCIPBindings.allocatePointerArray(arena, consVarPtrs);
        MemorySegment consValsSeg = SCIPBindings.allocateDoubleArray(arena, consVals);

        SCIPBindings.createConsBasicLinear(scip, consPtr, consName, nvars, consVarsSeg, consValsSeg, lhs, rhs);
    }

    private static void createQuadraticConstraint(final MemorySegment scip, final Arena arena, final MemorySegment consPtr, final MemorySegment consName,
            final Expression constraint, final MemorySegment[] varPtrs, final double lhs, final double rhs) {

        var linearKeys = constraint.getLinearKeySet();
        int nlin = linearKeys.size();

        MemorySegment[] linVars = new MemorySegment[nlin];
        double[] linCoefs = new double[nlin];

        int idx = 0;
        for (IntIndex key : linearKeys) {
            linVars[idx] = varPtrs[key.index];
            linCoefs[idx] = constraint.doubleValue(key, true);
            idx++;
        }

        var quadKeys = constraint.getQuadraticKeySet();
        List<MemorySegment> qv1 = new ArrayList<>();
        List<MemorySegment> qv2 = new ArrayList<>();
        List<Double> qc = new ArrayList<>();

        for (IntRowColumn key : quadKeys) {
            double val = constraint.doubleValue(key, true);
            if (val != ZERO) {
                qv1.add(varPtrs[key.row]);
                qv2.add(varPtrs[key.column]);
                qc.add(val);
            }
        }

        int nquad = qv1.size();

        MemorySegment linVarsSeg = SCIPBindings.allocatePointerArray(arena, linVars);
        MemorySegment linCoefsSeg = SCIPBindings.allocateDoubleArray(arena, linCoefs);
        MemorySegment qv1Seg = SCIPBindings.allocatePointerArray(arena, qv1.toArray(new MemorySegment[0]));
        MemorySegment qv2Seg = SCIPBindings.allocatePointerArray(arena, qv2.toArray(new MemorySegment[0]));
        double[] qcArr = new double[nquad];
        for (int i = 0; i < nquad; i++) {
            qcArr[i] = qc.get(i);
        }
        MemorySegment qcSeg = SCIPBindings.allocateDoubleArray(arena, qcArr);

        SCIPBindings.createConsBasicQuadraticNonlinear(scip, consPtr, consName, nlin, linVarsSeg, linCoefsSeg, nquad, qv1Seg, qv2Seg, qcSeg, lhs, rhs);
    }

    static Optimisation.State translate(final int status) {
        return switch (status) {
            case SCIPBindings.STATUS_OPTIMAL -> Optimisation.State.OPTIMAL;
            case SCIPBindings.STATUS_INFEASIBLE -> Optimisation.State.INFEASIBLE;
            case SCIPBindings.STATUS_UNBOUNDED -> Optimisation.State.UNBOUNDED;
            case SCIPBindings.STATUS_INFORUNBD -> Optimisation.State.INVALID;
            case SCIPBindings.STATUS_GAPLIMIT, SCIPBindings.STATUS_SOLLIMIT, SCIPBindings.STATUS_BESTSOLLIMIT,
                    SCIPBindings.STATUS_TIMELIMIT, SCIPBindings.STATUS_NODELIMIT,
                    SCIPBindings.STATUS_TOTALNODELIMIT, SCIPBindings.STATUS_STALLNODELIMIT,
                    SCIPBindings.STATUS_PRIMALLIMIT, SCIPBindings.STATUS_DUALLIMIT,
                    SCIPBindings.STATUS_RESTARTLIMIT -> Optimisation.State.APPROXIMATE;
            case SCIPBindings.STATUS_UNKNOWN -> Optimisation.State.UNEXPLORED;
            default -> Optimisation.State.FAILED;
        };
    }

    private final Arena myArena;
    private final int myNumVars;
    private final MemorySegment myScip;
    private final MemorySegment[] myVarPtrs;

    SolverSCIP(final MemorySegment scip, final MemorySegment[] varPtrs, final int numVars, final Arena arena) {
        super();
        myScip = scip;
        myVarPtrs = varPtrs;
        myNumVars = numVars;
        myArena = arena;
    }

    @Override
    public void dispose() {

        if (myScip != null && !myScip.equals(MemorySegment.NULL)) {
            MemorySegment scipPtr = myArena.allocate(ValueLayout.ADDRESS);
            scipPtr.set(ValueLayout.ADDRESS, 0, myScip);
            SCIPBindings.free(scipPtr);
        }
        myArena.close();
    }

    @Override
    public boolean fixVariable(final int index, final double value) {
        return this.updateRange(index, value, value);
    }

    @Override
    public double getDualMultiplier(final int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getReducedGradient(final int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optimisation.Result solve(final Result kickStarter) {

        SCIPBindings.solve(myScip);

        int status = SCIPBindings.getStatus(myScip);
        Optimisation.State state = SolverSCIP.translate(status);

        if (state.isFeasible() && state != Optimisation.State.UNBOUNDED) {

            MemorySegment sol = SCIPBindings.getBestSol(myScip);

            if (sol.equals(MemorySegment.NULL)) {
                return Optimisation.Result.of(state, new double[myNumVars]);
            }

            double objectiveValue = SCIPBindings.getSolOrigObj(myScip, sol);

            double[] solution = new double[myNumVars];
            for (int i = 0; i < myNumVars; i++) {
                solution[i] = SCIPBindings.getSolVal(myScip, sol, myVarPtrs[i]);
            }

            return Optimisation.Result.of(objectiveValue, state, solution);

        } else {

            return Optimisation.Result.of(state, new double[myNumVars]);
        }
    }

    @Override
    public boolean updateRange(final int index, final double lower, final double upper) {

        if (index < 0 || index >= myNumVars) {
            return false;
        }

        SCIPBindings.freeTransform(myScip);

        MemorySegment var = myVarPtrs[index];
        SCIPBindings.chgVarLb(myScip, var, lower);
        SCIPBindings.chgVarUb(myScip, var, upper);

        return true;
    }

}
