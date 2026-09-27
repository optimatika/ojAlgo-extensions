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
package org.ojalgo.optimisation.solver.joptimizer;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.ojalgo.array.ArrayR064;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Structure1D.IntIndex;
import org.ojalgo.structure.Structure2D.IntRowColumn;

import com.joptimizer.exception.InfeasibleProblemException;
import com.joptimizer.exception.JOptimizerException;
import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PSDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;
import com.joptimizer.optimizers.NewtonUnconstrained;
import com.joptimizer.optimizers.OptimizationRequest;
import com.joptimizer.optimizers.OptimizationRequestHandler;
import com.joptimizer.optimizers.OptimizationResponse;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public final class SolverJOptimizer implements Optimisation.Solver {

    public static class Configurator {

        protected void configure(final OptimizationRequest request, final OptimizationRequestHandler handler, final Optimisation.Options options) {

            PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n");

            Logger rootLogger = Logger.getRootLogger();
            rootLogger.setLevel(Level.DEBUG);
            if (options.logger_appender != null) {
                options.logger_appender.asWriter().ifPresent(a -> rootLogger.addAppender(new WriterAppender(layout, a)));
            }

            if (request instanceof LPOptimizationRequest) {
                ((LPOptimizationRequest) request).setPresolvingDisabled(true);
            }

        }

        protected OptimizationRequestHandler newOptimizationRequestHandler(final OptimizationRequest request) {
            return request instanceof LPOptimizationRequest ? new LPPrimalDualMethod() : new NewtonUnconstrained(true);
        }

    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverJOptimizer> {

        /**
         * Variable bounds are native bounds for linear problems, so this must be false. (For quadratic problems
         * they are inequality constraints, with coefficient 1 or the variable's adjustment factor.)
         */
        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = true;
        private static final boolean ADJUSTED_OBJECTIVE = true;

        Integration() {
            super();
        }

        @Override
        public SolverJOptimizer build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            boolean max = model.getOptimisationSense() == Optimisation.Sense.MAX;

            OptimizationRequest request;
            LPOptimizationRequest reqLP;

            if (model.isAnyExpressionQuadratic()) {
                request = new OptimizationRequest();
                reqLP = null;
            } else {
                reqLP = new LPOptimizationRequest();
                request = reqLP;
            }

            List<Variable> variables = model.getFreeVariables();
            int nbVars = variables.size();

            Set<IntIndex> fixed = model.getFixedVariables();

            Expression objective = model.objective().compensate(fixed);
            double objectiveScale = ADJUSTED_OBJECTIVE ? objective.getAdjustmentFactor() : ONE;
            ConvexMultivariateRealFunction objectiveFunction;

            List<Expression> equalities = model.constraints().filter(expr -> expr.isEqualityConstraint() && !expr.isAnyQuadraticFactorNonZero())
                    .map(expr -> expr.compensate(fixed)).collect(Collectors.toList());
            int nbEquals = equalities.size();

            if (nbEquals > 0) {

                DenseDoubleMatrix2D A = new DenseDoubleMatrix2D(nbEquals, nbVars);
                DenseDoubleMatrix1D b = new DenseDoubleMatrix1D(nbEquals);

                for (int i = 0; i < nbEquals; i++) {
                    Expression constr = equalities.get(i);

                    for (IntIndex key : constr.getLinearKeySet()) {
                        int freeIndex = model.indexOfFreeVariable(key);
                        if (freeIndex >= 0) {
                            A.set(i, freeIndex, constr.doubleValue(key, ADJUSTED_CONSTRAINTS));
                        }
                    }

                    b.set(i, constr.getLowerLimit(ADJUSTED_CONSTRAINTS, Double.NEGATIVE_INFINITY));
                }

                request.setA(A);
                request.setB(b);
            }

            if (reqLP != null) {
                // LP

                DenseDoubleMatrix1D c = new DenseDoubleMatrix1D(nbVars);
                DenseDoubleMatrix1D lb = new DenseDoubleMatrix1D(nbVars);
                DenseDoubleMatrix1D ub = new DenseDoubleMatrix1D(nbVars);

                for (int j = 0; j < nbVars; j++) {

                    Variable var = variables.get(j);

                    double low = var.getLowerLimit(ADJUSTED_BOUNDS, Double.NEGATIVE_INFINITY);
                    lb.setQuick(j, Double.isFinite(low) ? low : LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND);

                    double upp = var.getUpperLimit(ADJUSTED_BOUNDS, Double.POSITIVE_INFINITY);
                    ub.setQuick(j, Double.isFinite(upp) ? upp : LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND);
                }

                for (IntIndex key : objective.getLinearKeySet()) {

                    int freeIndex = model.indexOfFreeVariable(key);

                    if (freeIndex >= 0) {
                        double cost = objective.doubleValue(key, ADJUSTED_OBJECTIVE);
                        c.setQuick(freeIndex, max ? -cost : cost);
                    }
                }

                objectiveFunction = new LinearMultivariateRealFunction(c, ZERO);

                List<Expression> lowIneq = model.constraints().filter(expr -> expr.isLowerConstraint() && !expr.isAnyQuadraticFactorNonZero())
                        .map(expr -> expr.compensate(fixed)).collect(Collectors.toList());
                int nbLowIneq = lowIneq.size();

                List<Expression> uppIneq = model.constraints().filter(expr -> expr.isUpperConstraint() && !expr.isAnyQuadraticFactorNonZero())
                        .map(expr -> expr.compensate(fixed)).collect(Collectors.toList());
                int nbUppIneq = uppIneq.size();

                DenseDoubleMatrix2D G = new DenseDoubleMatrix2D(nbLowIneq + nbUppIneq, nbVars);
                DenseDoubleMatrix1D h = new DenseDoubleMatrix1D(nbLowIneq + nbUppIneq);

                for (int i = 0; i < nbLowIneq; i++) {
                    Expression constr = lowIneq.get(i);

                    for (IntIndex key : constr.getLinearKeySet()) {
                        int freeIndex = model.indexOfFreeVariable(key);
                        if (freeIndex >= 0) {
                            G.set(i, freeIndex, -constr.doubleValue(key, ADJUSTED_CONSTRAINTS));
                        }
                    }

                    h.set(i, -constr.getLowerLimit(ADJUSTED_CONSTRAINTS, Double.NEGATIVE_INFINITY));
                }

                for (int i = 0; i < nbUppIneq; i++) {
                    Expression constr = uppIneq.get(i);

                    for (IntIndex key : constr.getLinearKeySet()) {
                        int freeIndex = model.indexOfFreeVariable(key);
                        if (freeIndex >= 0) {
                            G.set(nbLowIneq + i, freeIndex, constr.doubleValue(key, ADJUSTED_CONSTRAINTS));
                        }
                    }

                    h.set(nbLowIneq + i, constr.getUpperLimit(ADJUSTED_CONSTRAINTS, Double.POSITIVE_INFINITY));
                }

                reqLP.setC(c);

                reqLP.setLb(lb);
                reqLP.setUb(ub);

                reqLP.setG(G);
                reqLP.setH(h);

            } else {
                // QP

                objectiveFunction = SolverJOptimizer.toObjectiveFunction(objective, nbVars, model, ADJUSTED_OBJECTIVE);
                request.setF0(objectiveFunction);

                // Inequality constraints, fi(x) <= 0

                List<ConvexMultivariateRealFunction> constrs = new ArrayList<>();

                model.constraints().filter(Expression::isLowerConstraint).map(expr -> expr.compensate(fixed))
                        .forEach(compC -> constrs.add(SolverJOptimizer.toLowerConstraint(compC, nbVars, model, ADJUSTED_CONSTRAINTS)));

                model.constraints().filter(Expression::isUpperConstraint).map(expr -> expr.compensate(fixed))
                        .forEach(compC -> constrs.add(SolverJOptimizer.toUpperConstraint(compC, nbVars, model, ADJUSTED_CONSTRAINTS)));

                for (int j = 0; j < nbVars; j++) {

                    Variable var = variables.get(j);
                    double coefficient = ADJUSTED_BOUNDS ? var.getAdjustmentFactor() : ONE;

                    if (var.isLowerLimitSet()) {
                        double[] q = new double[nbVars];
                        q[j] = -coefficient;
                        constrs.add(new LinearMultivariateRealFunction(q, var.getLowerLimit(ADJUSTED_BOUNDS, Double.NEGATIVE_INFINITY)));
                    }
                    if (var.isUpperLimitSet()) {
                        double[] q = new double[nbVars];
                        q[j] = coefficient;
                        constrs.add(new LinearMultivariateRealFunction(q, -var.getUpperLimit(ADJUSTED_BOUNDS, Double.POSITIVE_INFINITY)));
                    }
                }

                if (constrs.size() > 0) {
                    request.setFi(constrs.toArray(new ConvexMultivariateRealFunction[constrs.size()]));
                }
            }

            return new SolverJOptimizer(configurator, request, model.options, objectiveFunction, objectiveScale);
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger();
        }

        @Override
        public Result toModelState(final Result solverState, final ExpressionsBasedModel model) {
            return ExpressionsBasedModel.Integration.expandFreeToFull(solverState, model, ArrayR064.FACTORY, Optional.empty(), this.getSolverSense());
        }

        @Override
        public Result toSolverState(final Result modelState, final ExpressionsBasedModel model) {
            return ExpressionsBasedModel.Integration.reduceFullToFree(modelState, model, ArrayR064.FACTORY);
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return Optimisation.Sense.MIN;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverJOptimizer> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    private static ConvexMultivariateRealFunction toFunction(final Expression expression, final int dim, final boolean negate,
            final ExpressionsBasedModel model, final double constant, final boolean adjusted) {

        DenseDoubleMatrix2D p = null;
        DenseDoubleMatrix1D q = null;
        double r = constant;

        if (expression.isAnyQuadraticFactorNonZero()) {
            p = new DenseDoubleMatrix2D(dim, dim);

            for (IntRowColumn key : expression.getQuadraticKeySet()) {
                double val = expression.doubleValue(key, adjusted);
                double d = negate ? -val : val;
                int rowIndex = model.indexOfFreeVariable(key.row);
                int colIndex = model.indexOfFreeVariable(key.column);
                p.set(rowIndex, colIndex, p.get(rowIndex, colIndex) + d);
                p.set(colIndex, rowIndex, p.get(colIndex, rowIndex) + d);
            }

        }

        if (expression.isAnyLinearFactorNonZero()) {
            q = new DenseDoubleMatrix1D(dim);

            for (IntIndex key : expression.getLinearKeySet()) {
                double val = expression.doubleValue(key, adjusted);
                q.set(model.indexOfFreeVariable(key.index), negate ? -val : val);
            }
        }

        if (p != null) {
            return new PSDQuadraticMultivariateRealFunction(p, q, r);
        } else {
            return new LinearMultivariateRealFunction(q, r);
        }
    }

    /**
     * lower - g(x) &lt;= 0
     */
    static ConvexMultivariateRealFunction toLowerConstraint(final Expression expression, final int dim, final ExpressionsBasedModel model,
            final boolean adjusted) {
        return SolverJOptimizer.toFunction(expression, dim, true, model, expression.getLowerLimit(adjusted, Double.NEGATIVE_INFINITY), adjusted);
    }

    static ConvexMultivariateRealFunction toObjectiveFunction(final Expression expression, final int dim, final ExpressionsBasedModel model,
            final boolean adjusted) {
        return SolverJOptimizer.toFunction(expression, dim, model.getOptimisationSense() == Optimisation.Sense.MAX, model, 0.0, adjusted);
    }

    /**
     * g(x) - upper &lt;= 0
     */
    static ConvexMultivariateRealFunction toUpperConstraint(final Expression expression, final int dim, final ExpressionsBasedModel model,
            final boolean adjusted) {
        return SolverJOptimizer.toFunction(expression, dim, false, model, -expression.getUpperLimit(adjusted, Double.POSITIVE_INFINITY), adjusted);
    }

    private final Configurator myConfigurator;
    /**
     * The (minimisation form) objective function, of the free variables.
     */
    private final ConvexMultivariateRealFunction myObjective;
    /**
     * The objective's adjustment factor – the objective JOptimizer solves is scaled by this.
     */
    private final double myObjectiveScale;
    private final Optimisation.Options myOptions;
    private final OptimizationRequest myRequest;

    SolverJOptimizer(final Configurator configurator, final OptimizationRequest request, final Optimisation.Options options,
            final ConvexMultivariateRealFunction objective, final double objectiveScale) {

        super();

        myConfigurator = configurator;
        myRequest = request;
        myOptions = options;
        myObjective = objective;
        myObjectiveScale = objectiveScale;
    }

    @Override
    public Result solve(final Result kickStarter) {

        OptimizationRequestHandler handler = myConfigurator.newOptimizationRequestHandler(myRequest);

        myConfigurator.configure(myRequest, handler, myOptions);

        Optimisation.State retState = Optimisation.State.UNEXPLORED;
        double retValue = NaN;
        Access1D<Double> retSolution = null;

        //        if (kickStarter != null && kickStarter.getState().isFeasible()) {
        //            retState = kickStarter.getState();
        //            myRequest.setInitialPoint(kickStarter.toRawCopy1D());
        //        }

        handler.setOptimizationRequest(myRequest);

        try {

            handler.optimize();

            OptimizationResponse response = handler.getOptimizationResponse();

            retState = Optimisation.State.OPTIMAL;
            double[] solution = response.getSolution();
            retValue = myObjective.value(new DenseDoubleMatrix1D(solution)) / myObjectiveScale;
            retSolution = Access1D.wrap(solution);

        } catch (InfeasibleProblemException cause) {

            if (myOptions.logger_appender != null) {
                myOptions.logger_appender.print(cause.toString());
            }

            retState = Optimisation.State.INFEASIBLE;

        } catch (JOptimizerException cause) {

            if (myOptions.logger_appender != null) {
                myOptions.logger_appender.print(cause.toString());
            }

            retState = Optimisation.State.FAILED;

        } finally {

            if (retSolution == null && kickStarter != null) {
                retSolution = Access1D.wrap(kickStarter.toRawCopy1D());
            }
        }

        return new Optimisation.Result(retState, retValue, retSolution);
    }

}
