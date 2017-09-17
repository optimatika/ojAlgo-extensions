package org.ojalgo.nd4j.linalg.jvm;

import java.nio.IntBuffer;
import java.util.List;

import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.ShapeOffsetResolution;
import org.nd4j.linalg.indexing.conditions.Condition;
import org.ojalgo.array.ArrayAnyD;
import org.ojalgo.array.ArrayAnyD.Factory;
import org.ojalgo.array.ComplexArray;
import org.ojalgo.scalar.ComplexNumber;

public final class ComplexArrayND extends ArrayND<ComplexNumber> implements IComplexNDArray {

    public ComplexArrayND(final long count) {
        super(ComplexArray.FACTORY, count);
    }

    public ComplexArrayND(final long rows, final long columns) {
        super(ComplexArray.FACTORY, rows, columns);
    }

    public ComplexArrayND(final long[] structure) {
        super(ComplexArray.FACTORY, structure);
    }

    private ComplexArrayND(final Factory<ComplexNumber> factory, final ArrayAnyD<ComplexNumber> delegate) {
        super(factory, delegate);
    }

    @Override
    public IComplexNDArray add(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray add(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray add(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray add(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray add(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray add(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray add(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray addColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray addi(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray addi(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray addi(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray addi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray addi(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray addi(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray addi(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray addiColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray addiRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray addRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray assign(final IComplexNDArray arr) {
        // TODO Auto-generated method stub
        return null;
    }

    public void assign(final IComplexNumber aDouble) {
        // TODO Auto-generated method stub

    }

    @Override
    public INDArray assign(final INDArray arr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray assign(final Number value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray assignIf(final INDArray arr, final Condition condition) {
        // TODO Auto-generated method stub
        return null;
    }

    public int blasOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IComplexNDArray broadcast(final int[] shape) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void checkDimensions(final INDArray other) {
        // TODO Auto-generated method stub

    }

    @Override
    public void cleanup() {
        // TODO Auto-generated method stub

    }

    @Override
    public int columns() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IComplexNDArray cond(final Condition condition) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray condi(final Condition condition) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray conj() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray conji() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray cumsum(final int dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray cumsumi(final int dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataBuffer data() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray dimShuffle(final Object[] rearrange, final int[] newOrder, final boolean[] broadCastable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double distance1(final INDArray other) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double distance2(final INDArray other) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IComplexNDArray div(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray div(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray div(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray div(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray div(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray div(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray div(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray divColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray divi(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray divi(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray divi(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray divi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray divi(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray divi(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray divi(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray diviColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray diviRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray divRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray dup() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray dup(final char order) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object element() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int elementStride() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int elementWiseStride() {
        // TODO Auto-generated method stub
        return 0;
    }

    public IComplexNDArray eps(final IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray eps(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray eps(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray epsi(final IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray epsi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray epsi(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray eq(final IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray eq(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray eq(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray eqi(final IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray eqi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray eqi(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean equalsWithEps(final Object o, final double eps) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IComplexNDArray get(final INDArrayIndex... indexes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray getColumn(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray getColumns(final int[] columns) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber getComplex(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber getComplex(final int... indices) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber getComplex(final int i, final IComplexNumber result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber getComplex(final int i, final int j) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber getComplex(final int i, final int j, final IComplexNumber result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getDouble(final int... indices) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getDouble(final int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getDouble(final int i, final int j) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getDoubleUnsafe(final int offset) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getFloat(final int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getFloat(final int i, final int j) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getFloat(final int[] indices) {
        // TODO Auto-generated method stub
        return 0;
    }

    public double getImag(final int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getInt(final int... indices) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getLeadingOnes() {
        // TODO Auto-generated method stub
        return 0;
    }

    public INDArray getReal() {
        // TODO Auto-generated method stub
        return null;
    }

    public double getReal(final int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IComplexNDArray getRow(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray getRows(final int[] rows) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray getScalar(final int... indexes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray getScalar(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray getScalar(final int row, final int column) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getTrailingOnes() {
        // TODO Auto-generated method stub
        return 0;
    }

    public IComplexNDArray gt(final IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray gt(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray gt(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray gte(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray gtei(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray gti(final IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray gti(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray gti(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray hermitian() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray imag() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int index(final int row, final int column) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int innerMostStride() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isCleanedUp() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isColumnVector() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCompressed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isMatrix() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRowVector() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isScalar() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSquare() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isVector() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isView() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isWrapAround() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int length() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long lengthLong() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int linearIndex(final int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IComplexNDArray linearView() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray linearViewColumnOrder() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray lt(final IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray lt(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray lt(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray lte(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray ltei(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray lti(final IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray lti(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray lti(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int majorStride() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void markAsCompressed(final boolean reallyCompressed) {
        // TODO Auto-generated method stub

    }

    @Override
    public IComplexNDArray max(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNumber maxComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number maxNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mean(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNumber meanComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number meanNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray min(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNumber minComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number minNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mmul(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mmul(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mmuli(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mmuli(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mul(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mul(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray mul(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mul(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mul(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mul(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mul(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mulColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray muli(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray muli(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray muli(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray muli(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray muli(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray muli(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray muli(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray muliColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray muliRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray mulRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray neg() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray negi() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray neq(final IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray neq(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray neq(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray neqi(final IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray neqi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray neqi(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray norm1(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNumber norm1Complex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number norm1Number() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray norm2(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNumber norm2Complex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number norm2Number() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray normmax(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNumber normmaxComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number normmaxNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int offset() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public char ordering() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int originalOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IComplexNDArray permute(final int... rearrange) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray permutei(final int... rearrange) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray prod(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNumber prodComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number prodNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(final INDArrayIndex[] indices, final IComplexNDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(final INDArrayIndex[] indices, final IComplexNumber element) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray put(final INDArrayIndex[] indices, final INDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray put(final INDArrayIndex[] indices, final Number element) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(final int i, final IComplexNDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray put(final int i, final INDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(final int i, final int j, final IComplexNumber complex) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray put(final int i, final int j, final INDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray put(final int i, final int j, final Number element) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(final int[] indexes, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(final int[] indexes, final float value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray put(final int[] indices, final INDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray putColumn(final int column, final INDArray toPut) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putImag(final int i, final float v) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putImag(final int rowIndex, final int columnIndex, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putImag(final int rowIndex, final int columnIndex, final float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putImag(final int[] indices, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putImag(final int[] indices, final float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putReal(final int i, final float v) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putReal(final int rowIndex, final int columnIndex, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putReal(final int rowIndex, final int columnIndex, final float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putReal(final int[] indices, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putReal(final int[] indices, final float value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray putRow(final int row, final INDArray toPut) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray putScalar(final int i, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray putScalar(final int i, final float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putScalar(final int i, final IComplexNumber value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray putScalar(final int i, final int value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray putScalar(final int row, final int col, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putScalar(final int j, final int i, final IComplexNumber conji) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray putScalar(final int dim0, final int dim1, final int dim2, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray putScalar(final int dim0, final int dim1, final int dim2, final int dim3, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray putScalar(final int[] i, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray putScalar(final int[] indexes, final float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putScalar(final int[] i, final IComplexNumber complexNumber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray putScalar(final int[] indexes, final int value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray putScalarUnsafe(final int offset, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putSlice(final int slice, final IComplexNDArray put) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray putSlice(final int slice, final INDArray put) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int rank() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IComplexNDArray ravel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray ravel(final char order) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdiv(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdiv(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rdiv(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdiv(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdiv(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdiv(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdiv(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdivColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdivi(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdivi(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rdivi(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdivi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdivi(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdivi(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdivi(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdiviColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdiviRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rdivRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray real() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray repeat(final int... repeats) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray repeat(final int dimension, final int... repeats) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray replaceWhere(final INDArray arr, final Condition condition) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray repmat(final int... shape) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void resetLinearView() {
        // TODO Auto-generated method stub

    }

    @Override
    public IComplexNDArray reshape(final char order, final int... newShape) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray reshape(final char order, final int rows, final int columns) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray reshape(final int... newShape) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray reshape(final int rows, final int columns) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int rows() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IComplexNDArray rsub(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsub(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rsub(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsub(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsub(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsub(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsub(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsubColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsubi(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsubi(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rsubi(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsubi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsubi(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsubi(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsubi(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsubiColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsubiRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray rsubRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int secondaryStride() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setData(final DataBuffer data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setOrder(final char order) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setShape(final int... shape) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setStride(final int... stride) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setWrapAround(final boolean wrapAround) {
        // TODO Auto-generated method stub

    }

    @Override
    public int[] shape() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IntBuffer shapeInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataBuffer shapeInfoDataBuffer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String shapeInfoToString() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int size(final int dimension) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IComplexNDArray slice(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray slice(final int i, final int dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int slices() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void sliceVectors(final List<INDArray> list) {
        // TODO Auto-generated method stub

    }

    @Override
    public double squaredDistance(final INDArray other) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public INDArray std(final boolean biasCorrected, final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray std(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNumber stdComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number stdNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number stdNumber(final boolean biasCorrected) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int[] stride() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int stride(final int dimension) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IComplexNDArray sub(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray sub(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray sub(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray sub(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray sub(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray sub(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray sub(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray subArray(final int[] offsets, final int[] shape, final int[] stride) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray subArray(final ShapeOffsetResolution resolution) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray subColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray subi(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray subi(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray subi(final IComplexNumber n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray subi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray subi(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray subi(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray subi(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray subiColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray subiRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray subRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray sum(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNumber sumComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number sumNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray swapAxes(final int dimension, final int with) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray tensorAlongDimension(final int index, final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int tensorssAlongDimension(final int... dimension) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IComplexNDArray transpose() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray transposei() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray unsafeDuplication() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public INDArray var(final boolean biasCorrected, final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray var(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNumber varComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Number varNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IComplexNDArray vectorAlongDimension(final int index, final int dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int vectorsAlongDimension(final int dimension) {
        // TODO Auto-generated method stub
        return 0;
    }
}
