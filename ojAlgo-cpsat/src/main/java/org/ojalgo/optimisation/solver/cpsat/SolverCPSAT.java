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
package org.ojalgo.optimisation.solver.cpsat;

import static org.ojalgo.function.constant.PrimitiveMath.*;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.ojalgo.concurrent.InterruptForwarder;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Structure1D.IntIndex;

/**
 * Google CP-SAT solver integration for ojAlgo.
 * <p>
 * CP-SAT is Google OR-Tools' constraint programming / integer solver - one of the strongest solvers available
 * for pure integer and binary programs. This integration talks to it without the full OR-Tools Java jar
 * (which bundles JNI glue and native code for every OR-Tools component, not just CP-SAT): it uses Java's
 * Foreign Function &amp; Memory API to call the small, protobuf-based C API OR-Tools exports for exactly this
 * purpose ({@code ortools/sat/c_api/cp_solver_c.h}), compiled straight into {@code libortools}.
 * <p>
 * A model is translated into a serialized {@code CpModelProto} (see {@link #buildModelProtoBytes}) and handed
 * to the native solver together with a serialized {@code SatParameters}; the response comes back as a
 * serialized {@code CpSolverResponse} (see {@link #parseResponse}). Both are built/parsed by hand with a
 * minimal protobuf wire-format reader/writer - {@link ProtoWriter}/{@link ProtoReader} - rather than by
 * pulling in the protobuf-java runtime, since only a handful of scalar and length-delimited fields are ever
 * needed.
 * <p>
 * CP-SAT variables and coefficients are all 64-bit integers, whereas an {@link ExpressionsBasedModel} works
 * in {@code double}s. Variable bounds are simply rounded inwards ({@link Math#ceil} the lower bound,
 * {@link Math#floor} the upper bound) since CP-SAT has no notion of a continuous variable anyway.
 * Coefficients (in both constraints and the objective) are scaled up to the smallest power of ten that makes
 * them representable as integers - see {@link #scaleFactor}. For the objective this scaling is undone
 * automatically by the solver itself via {@code CpObjectiveProto.scaling_factor}, so the objective value
 * reported back is already in the model's own units.
 *
 * @see <a href="https://developers.google.com/optimization/cp/cp_solver">CP-SAT</a>
 */
public final class SolverCPSAT implements Optimisation.Solver {

    /**
     * Configuration options for the CP-SAT solver.
     * <p>
     * Subclass this to customize CP-SAT-specific settings (i.e. {@code SatParameters}). Set via
     * {@link ExpressionsBasedModel#setConfigurator(Object)} or
     * {@link Optimisation.Environment#setConfigurator(Object)}.
     */
    public static class Configurator {

        /**
         * Configure CP-SAT's {@code SatParameters} before solving.
         *
         * @param model      the expressions-based model
         * @param parameters the parameters to populate
         */
        protected void configure(final ExpressionsBasedModel model, final Parameters parameters) {

            Optimisation.Options options = model.options;

            long effectiveTimeMillis = Math.min(options.time_suffice, options.time_abort);
            if (effectiveTimeMillis > 0L && effectiveTimeMillis < Long.MAX_VALUE) {
                double seconds = effectiveTimeMillis / 1_000.0;
                parameters.setMaxTimeInSeconds(seconds);
            }

            long effectiveConflicts = Math.min((long) options.iterations_suffice, (long) options.iterations_abort);
            if (effectiveConflicts > 0L && effectiveConflicts < Long.MAX_VALUE) {
                parameters.setMaxNumberOfConflicts(effectiveConflicts);
            }

            int parallelism = options.getParallelism();
            if (parallelism > 1) {
                parameters.setNumWorkers(parallelism);
            }

            boolean verbose = options.logger_appender != null && options.logger_detailed;
            if (verbose) {
                parameters.setLogSearchProgress(true);
                parameters.setLogToStdout(true);
            }
        }

    }

    /**
     * A mutable {@code SatParameters} builder, populated by a {@link Configurator} and serialized once
     * {@link SolverCPSAT.Integration#build(ExpressionsBasedModel)} is done configuring it.
     * <p>
     * Field numbers are from {@code ortools/sat/sat_parameters.proto}. Only a handful of the (very many)
     * available parameters are exposed here - add more setters, following the same pattern, as the need
     * arises.
     */
    public static final class Parameters {

        private final ProtoWriter myWriter = new ProtoWriter();

        Parameters() {
            super();
        }

        /**
         * {@code log_search_progress = 41}
         */
        public Parameters setLogSearchProgress(final boolean value) {
            myWriter.bool(41, value);
            return this;
        }

        /**
         * {@code log_to_stdout = 186}
         */
        public Parameters setLogToStdout(final boolean value) {
            myWriter.bool(186, value);
            return this;
        }

        /**
         * {@code max_number_of_conflicts = 37}
         */
        public Parameters setMaxNumberOfConflicts(final long value) {
            myWriter.int64(37, value);
            return this;
        }

        /**
         * {@code max_time_in_seconds = 36}
         */
        public Parameters setMaxTimeInSeconds(final double value) {
            myWriter.fixed64Double(36, value);
            return this;
        }

        /**
         * {@code num_workers = 206}
         */
        public Parameters setNumWorkers(final int value) {
            myWriter.int32(206, value);
            return this;
        }

        /**
         * {@code random_seed = 31}
         */
        public Parameters setRandomSeed(final int value) {
            myWriter.int32(31, value);
            return this;
        }

        byte[] toByteArray() {
            return myWriter.toByteArray();
        }

    }

    /**
     * A minimal protobuf wire-format reader - just enough to pick the handful of fields
     * {@link SolverCPSAT#parseResponse(byte[])} needs out of a {@code CpSolverResponse}, skipping over
     * everything else. See {@link ProtoWriter} for the wire-format primitives this mirrors.
     */
    private static final class ProtoReader {

        private final byte[] myData;
        private int myPosition;

        ProtoReader(final byte[] data) {
            super();
            myData = data;
            myPosition = 0;
        }

        boolean hasRemaining() {
            return myPosition < myData.length;
        }

        double readFixed64Double() {
            long bits = 0L;
            for (int i = 0; i < 8; i++) {
                bits |= (myData[myPosition++] & 0xFFL) << (8 * i);
            }
            return Double.longBitsToDouble(bits);
        }

        byte[] readLengthDelimited() {
            int length = (int) this.readVarint();
            byte[] slice = Arrays.copyOfRange(myData, myPosition, myPosition + length);
            myPosition += length;
            return slice;
        }

        long readVarint() {
            long result = 0L;
            int shift = 0;
            while (true) {
                byte b = myData[myPosition++];
                result |= (b & 0x7FL) << shift;
                if ((b & 0x80) == 0) {
                    return result;
                }
                shift += 7;
            }
        }

        void skip(final int wireType) {
            switch (wireType) {
                case 0  -> this.readVarint();
                case 1  -> myPosition += 8;
                case 2  -> {
                    // Two separate statements, deliberately: "myPosition += (int) this.readVarint()" would
                    // capture the pre-call value of myPosition before evaluating the right-hand side, so the
                    // advance readVarint() itself makes while consuming the length prefix would be silently
                    // overwritten by the subsequent assignment - under-skipping by however many bytes the
                    // length varint occupied.
                    int length = (int) this.readVarint();
                    myPosition += length;
                }
                case 5  -> myPosition += 4;
                default -> throw new IllegalStateException("Unsupported protobuf wire type: " + wireType);
            }
        }

    }

    /**
     * A minimal protobuf wire-format writer - just enough to build the {@code CpModelProto} and
     * {@code SatParameters} messages CP-SAT's C API needs: varints, 64-bit doubles, length-delimited bytes
     * (used both for strings/submessages and for packed repeated scalar fields), nothing else. Deliberately
     * hand-rolled rather than pulling in the protobuf-java runtime, since the message shapes involved are
     * simple and fixed.
     * <p>
     * Proto3 scalar fields at their default value (0, 0.0, false) are omitted on the wire, matching what a
     * real protobuf implementation would produce; the {@code int32}/{@code int64}/{@code bool}/
     * {@code fixed64Double} setters below skip the write in that case.
     */
    private static final class ProtoWriter {

        private final ByteArrayOutputStream myBuffer = new ByteArrayOutputStream();

        ProtoWriter() {
            super();
        }

        void bool(final int fieldNumber, final boolean value) {
            if (value) {
                this.tag(fieldNumber, 0);
                this.varint(1L);
            }
        }

        void bytes(final int fieldNumber, final byte[] data) {
            this.tag(fieldNumber, 2);
            this.varint(data.length);
            myBuffer.writeBytes(data);
        }

        void fixed64Double(final int fieldNumber, final double value) {
            if (value != ZERO) {
                this.tag(fieldNumber, 1);
                long bits = Double.doubleToLongBits(value);
                for (int i = 0; i < 8; i++) {
                    myBuffer.write((int) (bits & 0xFF));
                    bits >>>= 8;
                }
            }
        }

        void int32(final int fieldNumber, final int value) {
            if (value != 0) {
                this.tag(fieldNumber, 0);
                this.varint(value);
            }
        }

        void int64(final int fieldNumber, final long value) {
            if (value != 0L) {
                this.tag(fieldNumber, 0);
                this.varint(value);
            }
        }

        /**
         * Embeds a submessage (length-delimited, wire type 2).
         */
        void message(final int fieldNumber, final ProtoWriter subMessage) {
            this.bytes(fieldNumber, subMessage.toByteArray());
        }

        /**
         * A packed {@code repeated int32} field: one length-delimited run of varints.
         */
        void packedInt32(final int fieldNumber, final int[] values) {
            if (values.length == 0) {
                return;
            }
            ProtoWriter packed = new ProtoWriter();
            for (int value : values) {
                packed.varint(value);
            }
            this.bytes(fieldNumber, packed.toByteArray());
        }

        /**
         * A packed {@code repeated int64} field: one length-delimited run of varints.
         */
        void packedInt64(final int fieldNumber, final long[] values) {
            if (values.length == 0) {
                return;
            }
            ProtoWriter packed = new ProtoWriter();
            for (long value : values) {
                packed.varint(value);
            }
            this.bytes(fieldNumber, packed.toByteArray());
        }

        void tag(final int fieldNumber, final int wireType) {
            this.varint((fieldNumber << 3) | wireType);
        }

        byte[] toByteArray() {
            return myBuffer.toByteArray();
        }

        /**
         * Standard protobuf base-128 varint encoding. {@code value} is treated as the raw 64-bit two's
         * complement pattern, exactly as plain (non-zigzag) {@code int32}/{@code int64} proto fields are - so
         * a negative {@code int32} (e.g. a negated variable reference) is sign-extended to 64 bits first and
         * correctly ends up as a full 10-byte varint, matching what a real protobuf implementation emits.
         */
        void varint(final long value) {
            long remaining = value;
            while (true) {
                if ((remaining & ~0x7FL) == 0L) {
                    myBuffer.write((int) remaining);
                    return;
                }
                myBuffer.write((int) ((remaining & 0x7F) | 0x80));
                remaining >>>= 7;
            }
        }

    }

    /**
     * The handful of {@code CpSolverResponse} fields this integration actually needs.
     */
    private static final class ResponseData {
        double bestObjectiveBound = ZERO;
        double objectiveValue = ZERO;
        long[] solution = {};
        int status = CPSATBindings.STATUS_UNKNOWN;
    }

    static final class Integration extends ExpressionsBasedModel.Integration<SolverCPSAT> {

        /**
         * CP-SAT works with all model variables directly - nothing is presolved away or shifted before it
         * gets there - so bounds, constraint limits and objective coefficients are all read unadjusted.
         */
        private static final boolean ADJUSTED_BOUNDS = false;
        private static final boolean ADJUSTED_CONSTRAINTS = false;
        private static final boolean ADJUSTED_OBJECTIVE = false;

        Integration() {
            super();
        }

        @Override
        public SolverCPSAT build(final ExpressionsBasedModel model) {

            Configurator configurator = model.getConfigurator(DEFAULT);

            List<Variable> variables = model.getVariables();
            List<Expression> constraints = model.constraints().toList();
            Expression objective = model.objective();

            Optimisation.Sense sense = model.getOptimisationSense();

            byte[] modelProtoBytes = SolverCPSAT.buildModelProtoBytes(variables, constraints, objective, sense, ADJUSTED_BOUNDS, ADJUSTED_CONSTRAINTS,
                    ADJUSTED_OBJECTIVE);

            Parameters parameters = new Parameters();
            configurator.configure(model, parameters);

            return new SolverCPSAT(modelProtoBytes, parameters.toByteArray(), variables.size(), model.options);
        }

        /**
         * CP-SAT is an integer/boolean solver - it has no notion of a continuous variable or of quadratic
         * terms (either in the objective or in a constraint).
         */
        @Override
        public boolean isCapable(final ExpressionsBasedModel model) {
            return model.isAnyVariableInteger() && !model.isAnyExpressionQuadratic();
        }

        @Override
        protected Optimisation.Sense getSolverSense() {
            return null;
        }

    }

    public static final ExpressionsBasedModel.Integration<SolverCPSAT> INTEGRATION = new Integration();

    static final Configurator DEFAULT = new Configurator();

    /**
     * A large-but-safe stand-in for +/- infinity in a CP-SAT domain. Kept well clear of the actual
     * {@code long} range limits (and of overflow when a scaled constraint bound is derived from it) rather
     * than using {@link Long#MAX_VALUE}/{@link Long#MIN_VALUE} directly, since CP-SAT's own internal
     * computations (presolve, sums of bounds, etc.) can overflow if domains sit right at the edge of the
     * representable range.
     */
    private static final long CPSAT_INFINITY = 1_000_000_000_000L;

    /**
     * Coefficients/bounds are scaled by at most {@code 10^MAX_SCALE_DIGITS}. Capped well short of what a
     * {@code long} can hold so that {@code coefficient * scale} cannot itself overflow for any coefficient
     * magnitude that would reasonably appear in a model.
     */
    private static final int MAX_SCALE_DIGITS = 9;

    /**
     * Builds the serialized {@code CpModelProto} (see {@code ortools/sat/cp_model.proto}) for the given
     * model. Field numbers below are from that proto:
     * <ul>
     * <li>{@code CpModelProto.variables = 2}, {@code .constraints = 3}, {@code .objective = 4}</li>
     * <li>{@code IntegerVariableProto.domain = 2}</li>
     * <li>{@code ConstraintProto.linear = 12} (the {@code oneof constraint} case used here)</li>
     * <li>{@code LinearConstraintProto.vars = 1}, {@code .coeffs = 2}, {@code .domain = 3}</li>
     * <li>{@code CpObjectiveProto.vars = 1}, {@code .coeffs = 4}, {@code .scaling_factor = 3}</li>
     * </ul>
     */
    private static byte[] buildModelProtoBytes(final List<Variable> variables, final List<Expression> constraints, final Expression objective,
            final Optimisation.Sense sense, final boolean adjustedBounds, final boolean adjustedConstraints, final boolean adjustedObjective) {

        ProtoWriter model = new ProtoWriter();

        for (Variable variable : variables) {

            double lower = variable.getLowerLimit(adjustedBounds, NEGATIVE_INFINITY);
            double upper = variable.getUpperLimit(adjustedBounds, POSITIVE_INFINITY);

            long lowerLong = SolverCPSAT.toDomainBound(lower, -CPSAT_INFINITY, true);
            long upperLong = SolverCPSAT.toDomainBound(upper, CPSAT_INFINITY, false);

            if (variable.isBinary()) {
                lowerLong = Math.max(lowerLong, 0L);
                upperLong = Math.min(upperLong, 1L);
            }

            if (lowerLong > upperLong) {
                throw new IllegalStateException("Variable '" + variable.getName() + "' has bounds [" + lower + ", " + upper
                        + "] with no representable integer value - CP-SAT only supports integer domains.");
            }

            ProtoWriter variableProto = new ProtoWriter();
            variableProto.packedInt64(2, new long[] { lowerLong, upperLong });
            model.message(2, variableProto);
        }

        for (Expression constraint : constraints) {

            double lower = constraint.getLowerLimit(adjustedConstraints, NEGATIVE_INFINITY);
            double upper = constraint.getUpperLimit(adjustedConstraints, POSITIVE_INFINITY);

            Set<IntIndex> keys = constraint.getLinearKeySet();
            int[] varIndices = new int[keys.size()];
            double[] rawCoeffs = new double[keys.size()];

            int i = 0;
            for (IntIndex key : keys) {
                varIndices[i] = key.index;
                rawCoeffs[i] = constraint.doubleValue(key, adjustedConstraints);
                i++;
            }

            double[] toScale = Arrays.copyOf(rawCoeffs, rawCoeffs.length + 2);
            toScale[rawCoeffs.length] = lower;
            toScale[rawCoeffs.length + 1] = upper;
            long factor = SolverCPSAT.scaleFactor(toScale);

            long[] coeffs = new long[rawCoeffs.length];
            for (int j = 0; j < rawCoeffs.length; j++) {
                coeffs[j] = SolverCPSAT.scaledRound(rawCoeffs[j], factor);
            }

            long lowerLong = SolverCPSAT.toScaledDomainBound(lower, -CPSAT_INFINITY, true, factor);
            long upperLong = SolverCPSAT.toScaledDomainBound(upper, CPSAT_INFINITY, false, factor);

            ProtoWriter linear = new ProtoWriter();
            linear.packedInt32(1, varIndices);
            linear.packedInt64(2, coeffs);
            linear.packedInt64(3, new long[] { lowerLong, upperLong });

            ProtoWriter constraintProto = new ProtoWriter();
            constraintProto.message(12, linear);

            model.message(3, constraintProto);
        }

        Set<IntIndex> objectiveKeys = objective.getLinearKeySet();
        if (!objectiveKeys.isEmpty()) {

            int[] objVarIndices = new int[objectiveKeys.size()];
            double[] rawObjCoeffs = new double[objectiveKeys.size()];

            int i = 0;
            for (IntIndex key : objectiveKeys) {
                objVarIndices[i] = key.index;
                rawObjCoeffs[i] = objective.doubleValue(key, adjustedObjective);
                i++;
            }

            long factor = SolverCPSAT.scaleFactor(rawObjCoeffs);

            // CP-SAT only ever minimises. For a MAX model the coefficients are negated and the scaling
            // factor's sign flips along with them - see CpObjectiveProto.scaling_factor's doc: "For a
            // maximization problem, one can negate all coefficients in the objective and set scaling_factor
            // to -1." The reported objective_value = scaling_factor * (sum(coeffs*vars) + offset) then comes
            // back already in the model's own sense and units, with no separate un-negation needed here.
            double sign = sense == Optimisation.Sense.MAX ? NEG : ONE;

            long[] objCoeffs = new long[rawObjCoeffs.length];
            for (int j = 0; j < rawObjCoeffs.length; j++) {
                objCoeffs[j] = SolverCPSAT.scaledRound(rawObjCoeffs[j] * sign, factor);
            }

            ProtoWriter objectiveProto = new ProtoWriter();
            objectiveProto.packedInt32(1, objVarIndices);
            objectiveProto.packedInt64(4, objCoeffs);
            objectiveProto.fixed64Double(3, sign / factor);

            model.message(4, objectiveProto);
        }

        return model.toByteArray();
    }

    /**
     * Parses a serialized {@code CpSolverResponse}, capturing only the fields this integration needs
     * ({@code status = 1}, {@code solution = 2}, {@code objective_value = 3}, {@code best_objective_bound =
     * 4}) and skipping everything else.
     */
    private static ResponseData parseResponse(final byte[] data) {

        ResponseData response = new ResponseData();

        if (data.length == 0) {
            return response;
        }

        ProtoReader reader = new ProtoReader(data);

        while (reader.hasRemaining()) {

            long tag = reader.readVarint();
            int fieldNumber = (int) (tag >>> 3);
            int wireType = (int) (tag & 0x7);

            switch (fieldNumber) {
                case 1  -> response.status = (int) reader.readVarint();
                case 2  -> {
                    if (wireType == 2) {
                        byte[] packed = reader.readLengthDelimited();
                        ProtoReader packedReader = new ProtoReader(packed);
                        List<Long> values = new ArrayList<>();
                        while (packedReader.hasRemaining()) {
                            values.add(packedReader.readVarint());
                        }
                        long[] solution = new long[values.size()];
                        for (int i = 0; i < solution.length; i++) {
                            solution[i] = values.get(i);
                        }
                        response.solution = solution;
                    } else {
                        // Unpacked entry (not what the C++ implementation emits for a proto3 repeated
                        // scalar, but valid on the wire regardless) - handled defensively.
                        reader.skip(wireType);
                    }
                }
                case 3  -> response.objectiveValue = reader.readFixed64Double();
                case 4  -> response.bestObjectiveBound = reader.readFixed64Double();
                default -> reader.skip(wireType);
            }
        }

        return response;
    }

    private static long scaledRound(final double value, final long factor) {
        return Math.round(value * factor);
    }

    /**
     * The smallest {@code 10^k} (k from 0 up to {@link #MAX_SCALE_DIGITS}) such that every finite, non-zero
     * value in {@code values}, multiplied by it, is (at least very nearly) an integer - determined from each
     * value's minimal decimal representation via {@link BigDecimal}. Infinite and zero entries are ignored
     * (zero needs no scaling to become the integer 0; an infinite bound is handled separately, as the CP-SAT
     * domain's own +/-infinity sentinel).
     */
    private static long scaleFactor(final double... values) {

        long factor = 1L;

        for (double value : values) {

            if (value == ZERO || !Double.isFinite(value)) {
                continue;
            }

            int decimals = Math.min(Math.max(BigDecimal.valueOf(value).stripTrailingZeros().scale(), 0), MAX_SCALE_DIGITS);

            long candidate = 1L;
            for (int i = 0; i < decimals; i++) {
                candidate *= 10L;
            }

            if (candidate > factor) {
                factor = candidate;
            }
        }

        return factor;
    }

    /**
     * Rounds a (possibly infinite) bound to a CP-SAT domain endpoint, without any coefficient scaling - used
     * for variable bounds, which CP-SAT always expresses 1:1 as integers. Rounds inwards (ceiling for a lower
     * bound, floor for an upper bound) so the resulting integer domain never admits a value the original,
     * possibly fractional, bound excluded.
     */
    private static long toDomainBound(final double value, final long infinityValue, final boolean isLower) {

        if (!Double.isFinite(value)) {
            return infinityValue;
        }

        double clamped = Math.max(-CPSAT_INFINITY, Math.min(CPSAT_INFINITY, value));

        return isLower ? (long) Math.ceil(clamped) : (long) Math.floor(clamped);
    }

    /**
     * As {@link #toDomainBound(double, long, boolean)}, but for a constraint bound that must additionally be
     * multiplied by that row's integer coefficient scale factor before rounding.
     */
    private static long toScaledDomainBound(final double value, final long infinityValue, final boolean isLower, final long factor) {

        if (!Double.isFinite(value)) {
            return infinityValue;
        }

        double scaled = value * factor;
        double bound = CPSAT_INFINITY * (double) factor;
        double clamped = Math.max(-bound, Math.min(bound, scaled));

        return isLower ? (long) Math.ceil(clamped) : (long) Math.floor(clamped);
    }

    /**
     * Translates a {@code CpSolverStatus} (see {@code cp_model.proto}) to an ojAlgo
     * {@link Optimisation.State}.
     */
    static Optimisation.State translate(final int status) {
        return switch (status) {
            case CPSATBindings.STATUS_OPTIMAL       -> Optimisation.State.OPTIMAL;
            case CPSATBindings.STATUS_FEASIBLE      -> Optimisation.State.FEASIBLE;
            case CPSATBindings.STATUS_INFEASIBLE    -> Optimisation.State.INFEASIBLE;
            case CPSATBindings.STATUS_MODEL_INVALID -> Optimisation.State.INVALID;
            case CPSATBindings.STATUS_UNKNOWN       -> Optimisation.State.UNEXPLORED;
            default                                 -> Optimisation.State.FAILED;
        };
    }

    private final MemorySegment myEnv;
    private final int myIterationsAbort;
    private final int myIterationsSuffice;
    private final byte[] myModelProtoBytes;
    private final int myNumVars;
    private final byte[] myParamsProtoBytes;
    private final long myTimeAbortMillis;
    private final long myTimeSufficeMillis;

    SolverCPSAT(final byte[] modelProtoBytes, final byte[] paramsProtoBytes, final int numVars, final Optimisation.Options options) {
        super();
        myModelProtoBytes = modelProtoBytes;
        myParamsProtoBytes = paramsProtoBytes;
        myNumVars = numVars;
        myIterationsAbort = options.iterations_abort;
        myIterationsSuffice = options.iterations_suffice;
        myTimeAbortMillis = options.time_abort;
        myTimeSufficeMillis = options.time_suffice;
        myEnv = CPSATBindings.newEnv();
    }

    @Override
    public void dispose() {
        if (myEnv != null && !myEnv.equals(MemorySegment.NULL)) {
            CPSATBindings.destroyEnv(myEnv);
        }
    }

    @Override
    public Optimisation.Result solve(final Optimisation.Result kickStarter) {

        long startMillis = System.currentTimeMillis();

        byte[] responseBytes;
        try (InterruptForwarder interrupter = InterruptForwarder.watch(() -> CPSATBindings.stopSearch(myEnv))) {
            responseBytes = CPSATBindings.solveInterruptible(myEnv, myModelProtoBytes, myParamsProtoBytes);
        }

        ResponseData response = SolverCPSAT.parseResponse(responseBytes);
        Optimisation.State state = SolverCPSAT.translate(response.status);

        if (!state.isFeasible() && myTimeSufficeMillis < myTimeAbortMillis) {
            long elapsed = System.currentTimeMillis() - startMillis;
            long remaining = myTimeAbortMillis - elapsed;
            if (remaining > 1000L) {

                // Build override parameters for the remaining time (and possibly the abort-level conflict
                // limit). Protobuf's "last field wins" semantics for scalar fields means we can simply
                // concatenate the original serialized parameters with a small override message - the later
                // field values supersede the earlier ones.
                Parameters overrides = new Parameters();
                overrides.setMaxTimeInSeconds(remaining / 1000.0);
                if (myIterationsSuffice < myIterationsAbort) {
                    overrides.setMaxNumberOfConflicts(myIterationsAbort);
                }

                byte[] overrideBytes = overrides.toByteArray();
                byte[] phase2Params = new byte[myParamsProtoBytes.length + overrideBytes.length];
                System.arraycopy(myParamsProtoBytes, 0, phase2Params, 0, myParamsProtoBytes.length);
                System.arraycopy(overrideBytes, 0, phase2Params, myParamsProtoBytes.length, overrideBytes.length);

                try (InterruptForwarder interrupter = InterruptForwarder.watch(() -> CPSATBindings.stopSearch(myEnv))) {
                    responseBytes = CPSATBindings.solveInterruptible(myEnv, myModelProtoBytes, phase2Params);
                }

                response = SolverCPSAT.parseResponse(responseBytes);
                state = SolverCPSAT.translate(response.status);
            }
        }

        if (state.isFeasible()) {

            double[] solution = new double[myNumVars];
            int count = Math.min(myNumVars, response.solution.length);
            for (int i = 0; i < count; i++) {
                solution[i] = response.solution[i];
            }

            return Optimisation.Result.of(response.objectiveValue, state, solution);

        } else {

            return Optimisation.Result.of(state, new double[myNumVars]);
        }
    }

}
