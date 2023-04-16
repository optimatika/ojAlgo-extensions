/*
 * Copyright 1997-2023 Optimatika
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

import java.util.List;
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

    @FunctionalInterface
    public interface Configurator {

        void configure(OptimizationRequest request, OptimizationRequestHandler handler, Optimisation.Options options);

    }

    public static final class Integration extends ExpressionsBasedModel.Integration<SolverJOptimizer> {

        Integration() {
            super();
        }

        @Override
        public SolverJOptimizer build(final ExpressionsBasedModel model) {

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
            int numbVars = variables.size();

            Set<IntIndex> fixed = model.getFixedVariables();

            Expression objective = model.objective();

            List<Expression> equalities = model.constraints().filter(expr -> expr.isEqualityConstraint() && !expr.isAnyQuadraticFactorNonZero())
                    .collect(Collectors.toList());
            int numbEquals = equalities.size();

            if (numbEquals > 0) {

                DenseDoubleMatrix2D A = new DenseDoubleMatrix2D(numbEquals, numbVars);
                DenseDoubleMatrix1D b = new DenseDoubleMatrix1D(numbEquals);

                for (int i = 0; i < numbEquals; i++) {
                    Expression constr = equalities.get(i).compensate(fixed);

                    for (IntIndex key : constr.getLinearKeySet()) {
                        int freeIndex = model.indexOfFreeVariable(key);
                        if (freeIndex >= 0) {
                            A.set(i, freeIndex, constr.getAdjustedLinearFactor(key));
                        }
                    }

                    b.set(i, constr.getAdjustedLowerLimit());
                }

                request.setA(A);
                request.setB(b);
            }

            if (reqLP != null) {
                // LP

                DenseDoubleMatrix1D c = new DenseDoubleMatrix1D(numbVars);
                DenseDoubleMatrix1D lb = new DenseDoubleMatrix1D(numbVars);
                DenseDoubleMatrix1D ub = new DenseDoubleMatrix1D(numbVars);

                lb.assign(LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND);
                ub.assign(LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND);

                for (IntIndex key : objective.getLinearKeySet()) {

                    int freeIndex = model.indexOfFreeVariable(key);

                    if (freeIndex >= 0) {

                        Variable var = model.getVariable(key);

                        double cost = objective.getAdjustedLinearFactor(freeIndex);
                        c.setQuick(freeIndex, max ? -cost : cost);

                        double low = var.getUnadjustedLowerLimit();
                        lb.setQuick(freeIndex, Double.isFinite(low) ? low : LPPrimalDualMethod.DEFAULT_MIN_LOWER_BOUND);

                        double upp = var.getUnadjustedUpperLimit();
                        ub.setQuick(freeIndex, Double.isFinite(upp) ? upp : LPPrimalDualMethod.DEFAULT_MAX_UPPER_BOUND);
                    }
                }

                List<Expression> lowIneq = model.constraints().filter(expr -> expr.isLowerConstraint() && !expr.isAnyQuadraticFactorNonZero())
                        .collect(Collectors.toList());
                int numbLowIneq = lowIneq.size();

                List<Expression> uppIneq = model.constraints().filter(expr -> expr.isUpperConstraint() && !expr.isAnyQuadraticFactorNonZero())
                        .collect(Collectors.toList());
                int numbUppIneq = uppIneq.size();

                DenseDoubleMatrix2D G = new DenseDoubleMatrix2D(numbLowIneq + numbUppIneq, numbVars);
                DenseDoubleMatrix1D h = new DenseDoubleMatrix1D(numbLowIneq + numbUppIneq);

                for (int i = 0; i < numbLowIneq; i++) {
                    Expression constr = lowIneq.get(i).compensate(fixed);

                    for (IntIndex key : constr.getLinearKeySet()) {
                        int freeIndex = model.indexOfFreeVariable(key);
                        if (freeIndex >= 0) {
                            G.set(i, freeIndex, -constr.getAdjustedLinearFactor(key));
                        }
                    }

                    h.set(i, -constr.getAdjustedLowerLimit());
                }

                for (int i = 0; i < numbUppIneq; i++) {
                    Expression constr = uppIneq.get(i).compensate(fixed);

                    for (IntIndex key : constr.getLinearKeySet()) {
                        int freeIndex = model.indexOfFreeVariable(key);
                        if (freeIndex >= 0) {
                            G.set(numbLowIneq + i, freeIndex, constr.getAdjustedLinearFactor(key));
                        }
                    }

                    h.set(numbLowIneq + i, constr.getAdjustedUpperLimit());
                }

                reqLP.setC(c);

                reqLP.setLb(lb);
                reqLP.setUb(ub);

                reqLP.setG(G);
                reqLP.setH(h);

            } else {
                // QP

                Expression compObj = objective.compensate(fixed);
                ConvexMultivariateRealFunction objFunc = SolverJOptimizer.toObjectiveFunction(compObj, numbVars, model);
                request.setF0(objFunc);

                List<Expression> lowerConstr = model.constraints().filter(Expression::isLowerConstraint).collect(Collectors.toList());
                int numbLowerConstr = lowerConstr.size();

                List<Expression> upperConstr = model.constraints().filter(Expression::isUpperConstraint).collect(Collectors.toList());
                int numbUpperConstr = upperConstr.size();

                if (numbLowerConstr + numbUpperConstr > 0) {

                    ConvexMultivariateRealFunction[] constrs = new ConvexMultivariateRealFunction[numbLowerConstr + numbUpperConstr];

                    for (int i = 0; i < numbLowerConstr; i++) {
                        Expression compC = lowerConstr.get(0).compensate(fixed);
                        ConvexMultivariateRealFunction c = SolverJOptimizer.toLowerConstraint(compC, numbVars, model);
                        constrs[i] = c;
                    }
                    for (int i = 0; i < numbUpperConstr; i++) {
                        Expression compC = upperConstr.get(0).compensate(fixed);
                        ConvexMultivariateRealFunction c = SolverJOptimizer.toUpperConstraint(compC, numbVars, model);
                        constrs[numbLowerConstr + i] = c;
                    }

                    request.setFi(constrs);
                }

            }

            return new SolverJOptimizer(request, model.options);
        }

        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return !model.isAnyVariableInteger();
        }

        @Override
        public Result toModelState(final Result solverState, final ExpressionsBasedModel model) {

            List<Variable> freeVariables = model.getFreeVariables();
            Set<IntIndex> fixedVariables = model.getFixedVariables();
            int nbFreeVars = freeVariables.size();
            int nbModelVars = model.countVariables();

            ArrayR064 modelSolution = ArrayR064.make(nbModelVars);

            for (int i = 0; i < nbFreeVars; i++) {
                modelSolution.set(model.indexOf(freeVariables.get(i)), solverState.doubleValue(i));
            }

            for (IntIndex fixed : fixedVariables) {
                modelSolution.set(fixed.index, model.getVariable(fixed.index).getValue());
            }

            return new Result(solverState.getState(), modelSolution);
        }

        @Override
        public Result toSolverState(final Result modelState, final ExpressionsBasedModel model) {

            List<Variable> freeVariables = model.getFreeVariables();
            int nbFreeVars = freeVariables.size();

            ArrayR064 solverSolution = ArrayR064.make(nbFreeVars);

            for (int i = 0; i < nbFreeVars; i++) {
                Variable variable = freeVariables.get(i);
                int modelIndex = model.indexOf(variable);
                solverSolution.set(i, modelState.doubleValue(modelIndex));
            }

            return new Result(modelState.getState(), solverSolution);
        }

    }

    public static final SolverJOptimizer.Integration INTEGRATION = new Integration();

    static final Configurator DEFAULT = (request, handler, options) -> {

        PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n");

        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.DEBUG);
        if (options.logger_appender != null) {
            options.logger_appender.asWriter().ifPresent(a -> rootLogger.addAppender(new WriterAppender(layout, a)));
        }

        if (request instanceof LPOptimizationRequest) {
            ((LPOptimizationRequest) request).setPresolvingDisabled(true);
        }

    };

    private static ConvexMultivariateRealFunction toFunction(final Expression expression, final int dim, final boolean negate,
            final ExpressionsBasedModel model, final double constant) {

        DenseDoubleMatrix2D p = null;
        DenseDoubleMatrix1D q = null;
        double r = constant;

        if (expression.isAnyQuadraticFactorNonZero()) {
            p = new DenseDoubleMatrix2D(dim, dim);

            for (IntRowColumn key : expression.getQuadraticKeySet()) {
                double adjusted = expression.getAdjustedQuadraticFactor(key);
                double d = negate ? -adjusted : adjusted;
                int rowIndex = model.indexOfFreeVariable(key.row);
                int colIndex = model.indexOfFreeVariable(key.column);
                p.set(rowIndex, colIndex, p.get(rowIndex, colIndex) + d);
                p.set(colIndex, rowIndex, p.get(colIndex, rowIndex) + d);
            }

        }

        if (expression.isAnyLinearFactorNonZero()) {
            q = new DenseDoubleMatrix1D(dim);

            for (IntIndex key : expression.getLinearKeySet()) {
                double adjusted = expression.getAdjustedLinearFactor(key);
                q.set(model.indexOfFreeVariable(key.index), negate ? -adjusted : adjusted);
            }
        }

        if (p != null) {
            return new PSDQuadraticMultivariateRealFunction(p, q, r);
        }
        return new LinearMultivariateRealFunction(q, r);

    }

    static ConvexMultivariateRealFunction toLowerConstraint(final Expression expression, final int dim, final ExpressionsBasedModel model) {
        return SolverJOptimizer.toFunction(expression, dim, false, model, expression.getAdjustedLowerLimit());
    }

    static ConvexMultivariateRealFunction toObjectiveFunction(final Expression expression, final int dim, final ExpressionsBasedModel model) {
        return SolverJOptimizer.toFunction(expression, dim, model.getOptimisationSense() == Optimisation.Sense.MAX, model, 0.0);
    }

    static ConvexMultivariateRealFunction toUpperConstraint(final Expression expression, final int dim, final ExpressionsBasedModel model) {
        return SolverJOptimizer.toFunction(expression, dim, false, model, -expression.getAdjustedUpperLimit());
    }

    private final Optimisation.Options myOptions;
    private final OptimizationRequest myRequest;

    SolverJOptimizer(final OptimizationRequest request, final Optimisation.Options options) {
        super();
        myRequest = request;
        myOptions = options;
    }

    @Override
    public Result solve(final Result kickStarter) {

        OptimizationRequestHandler handler = myRequest instanceof LPOptimizationRequest ? new LPPrimalDualMethod() : new NewtonUnconstrained(true);

        DEFAULT.configure(myRequest, handler, myOptions);
        myOptions.getConfigurator(Configurator.class).ifPresent(c -> c.configure(myRequest, handler, myOptions));

        Optimisation.State retState = Optimisation.State.UNEXPLORED;
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
            retSolution = Access1D.wrap(response.getSolution());

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

        return new Optimisation.Result(retState, retSolution);
    }

}
