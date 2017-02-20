/*
 * Copyright 1997-2017 Optimatika (www.optimatika.se)
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
package org.ojalgo.linalg.jvm;

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

abstract class AbstractND<N extends Number, I extends AbstractND<N, I>> implements INDArray {

    private final ArrayAnyD<N> myDelegate = null;

    AbstractND() {
        super();
    }

    public String shapeInfoToString() {
        // TODO Auto-generated method stub
        return null;
    }

    public DataBuffer shapeInfoDataBuffer() {
        // TODO Auto-generated method stub
        return null;
    }

    public IntBuffer shapeInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isView() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isCompressed() {
        // TODO Auto-generated method stub
        return false;
    }

    public void markAsCompressed(final boolean reallyCompressed) {
        // TODO Auto-generated method stub

    }

    public void setWrapAround(final boolean wrapAround) {
        // TODO Auto-generated method stub

    }

    public boolean isWrapAround() {
        // TODO Auto-generated method stub
        return false;
    }

    public int rank() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int stride(final int dimension) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int elementStride() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int elementWiseStride() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isCleanedUp() {
        // TODO Auto-generated method stub
        return false;
    }

    public void cleanup() {
        // TODO Auto-generated method stub

    }

    public void resetLinearView() {
        // TODO Auto-generated method stub

    }

    public int secondaryStride() {
        // TODO Auto-generated method stub
        return 0;
    }

    public double getDoubleUnsafe(final int offset) {
        // TODO Auto-generated method stub
        return 0;
    }

    public INDArray putScalarUnsafe(final int offset, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public int majorStride() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int innerMostStride() {
        // TODO Auto-generated method stub
        return 0;
    }

    public INDArray linearView() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray linearViewColumnOrder() {
        // TODO Auto-generated method stub
        return null;
    }

    public int vectorsAlongDimension(final int dimension) {
        // TODO Auto-generated method stub
        return 0;
    }

    public INDArray vectorAlongDimension(final int index, final int dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public int tensorssAlongDimension(final int... dimension) {
        // TODO Auto-generated method stub
        return 0;
    }

    public INDArray tensorAlongDimension(final int index, final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray cumsumi(final int dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray cumsum(final int dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray assign(final INDArray arr) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray assignIf(final INDArray arr, final Condition condition) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray replaceWhere(final INDArray arr, final Condition condition) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putScalar(final int i, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putScalar(final int i, final float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putScalar(final int i, final int value) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putScalar(final int[] i, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putScalar(final int row, final int col, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putScalar(final int dim0, final int dim1, final int dim2, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putScalar(final int dim0, final int dim1, final int dim2, final int dim3, final double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray lt(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray lti(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putScalar(final int[] indexes, final float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putScalar(final int[] indexes, final int value) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray eps(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray epsi(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray eq(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray eqi(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray gt(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray gte(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray lte(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray gtei(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray ltei(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray gti(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray lt(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray lti(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray eps(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray epsi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray neq(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray neqi(final Number other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray neq(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray neqi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray eq(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray eqi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray gt(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray gti(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray neg() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray negi() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdiv(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdivi(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsub(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsubi(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray div(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray divi(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray mul(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray muli(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray sub(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray subi(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray add(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray addi(final Number n) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdiv(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdivi(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsub(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsubi(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray div(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray divi(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray mul(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray muli(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray sub(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray subi(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray add(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray addi(final Number n, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray get(final INDArrayIndex... indexes) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray getColumns(final int... columns) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray getRows(final int... rows) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdiv(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdivi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdiv(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdivi(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsub(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsub(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsubi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsubi(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray assign(final Number value) {
        // TODO Auto-generated method stub
        myDelegate.fillAll((N) value);
        return this;
    }

    public int linearIndex(final int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void checkDimensions(final INDArray other) {
        // TODO Auto-generated method stub

    }

    public void sliceVectors(final List<INDArray> list) {
        // TODO Auto-generated method stub

    }

    public INDArray putSlice(final int slice, final INDArray put) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray cond(final Condition condition) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray condi(final Condition condition) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray repmat(final int... shape) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray repeat(final int dimension, final int... repeats) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray repeat(final int... repeats) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putRow(final int row, final INDArray toPut) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putColumn(final int column, final INDArray toPut) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray getScalar(final int row, final int column) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray getScalar(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public int index(final int row, final int column) {
        // TODO Auto-generated method stub
        return 0;
    }

    public double squaredDistance(final INDArray other) {
        // TODO Auto-generated method stub
        return 0;
    }

    public double distance2(final INDArray other) {
        // TODO Auto-generated method stub
        return 0;
    }

    public double distance1(final INDArray other) {
        // TODO Auto-generated method stub
        return 0;
    }

    public INDArray put(final INDArrayIndex[] indices, final INDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray put(final INDArrayIndex[] indices, final Number element) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray put(final int[] indices, final INDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray put(final int i, final int j, final INDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray put(final int i, final int j, final Number element) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray put(final int i, final INDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray diviColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray divColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray diviRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray divRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdiviColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdivColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdiviRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rdivRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray muliColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray mulColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray muliRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray mulRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsubiColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsubColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsubiRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray rsubRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray subiColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray subColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray subiRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray subRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray addiColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray addColumnVector(final INDArray columnVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray addiRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray addRowVector(final INDArray rowVector) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray mmul(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray mmul(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray div(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray div(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray mul(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray mul(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray sub(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray sub(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray add(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray add(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray mmuli(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray mmuli(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray divi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray divi(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray muli(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray muli(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray subi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray subi(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray addi(final INDArray other) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray addi(final INDArray other, final INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray normmax(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number normmaxNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber normmaxComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray norm2(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number norm2Number() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber norm2Complex() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray norm1(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number norm1Number() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber norm1Complex() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray std(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number stdNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray std(final boolean biasCorrected, final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number stdNumber(final boolean biasCorrected) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber stdComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray prod(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number prodNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber prodComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray mean(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number meanNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber meanComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray var(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray var(final boolean biasCorrected, final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number varNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber varComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray max(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number maxNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber maxComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray min(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number minNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber minComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray sum(final int... dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public Number sumNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber sumComplex() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setStride(final int... stride) {
        // TODO Auto-generated method stub

    }

    public void setShape(final int... shape) {
        // TODO Auto-generated method stub

    }

    public void setOrder(final char order) {
        // TODO Auto-generated method stub

    }

    public INDArray subArray(final ShapeOffsetResolution resolution) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray subArray(final int[] offsets, final int[] shape, final int[] stride) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray getScalar(final int... indices) {
        // TODO Auto-generated method stub
        return null;
    }

    public int getInt(final int... indices) {
        // TODO Auto-generated method stub
        return 0;
    }

    public double getDouble(final int... indices) {
        // TODO Auto-generated method stub
        return 0;
    }

    public float getFloat(final int[] indices) {
        // TODO Auto-generated method stub
        return 0;
    }

    public double getDouble(final int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    public double getDouble(final int i, final int j) {
        // TODO Auto-generated method stub
        return 0;
    }

    public float getFloat(final int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    public float getFloat(final int i, final int j) {
        // TODO Auto-generated method stub
        return 0;
    }

    public INDArray dup() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray dup(final char order) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray ravel() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray ravel(final char order) {
        // TODO Auto-generated method stub
        return null;
    }

    public void setData(final DataBuffer data) {
        // TODO Auto-generated method stub

    }

    public int slices() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getTrailingOnes() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getLeadingOnes() {
        // TODO Auto-generated method stub
        return 0;
    }

    public INDArray slice(final int i, final int dimension) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray slice(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public int offset() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int originalOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    public INDArray reshape(final char order, final int... newShape) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray reshape(final char order, final int rows, final int columns) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray reshape(final int... newShape) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray reshape(final int rows, final int columns) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray transpose() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray transposei() {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray swapAxes(final int dimension, final int with) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray permute(final int... rearrange) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray permutei(final int... rearrange) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray dimShuffle(final Object[] rearrange, final int[] newOrder, final boolean[] broadCastable) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray getColumn(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray getRow(final int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public int columns() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int rows() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isColumnVector() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isRowVector() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isVector() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isSquare() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isMatrix() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isScalar() {
        // TODO Auto-generated method stub
        return false;
    }

    public int[] shape() {
        // TODO Auto-generated method stub
        return null;
    }

    public int[] stride() {
        // TODO Auto-generated method stub
        return null;
    }

    public char ordering() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int size(final int dimension) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int length() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long lengthLong() {
        // TODO Auto-generated method stub
        return 0;
    }

    public INDArray broadcast(final int... shape) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object element() {
        // TODO Auto-generated method stub
        return null;
    }

    public DataBuffer data() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rdiv(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rdivi(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rsub(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rsubi(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray div(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray divi(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray mul(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray muli(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray sub(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray subi(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray add(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray addi(final IComplexNumber n) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rdiv(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rdivi(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rsub(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rsubi(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray div(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray divi(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray mul(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray muli(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray sub(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray subi(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray add(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray addi(final IComplexNumber n, final IComplexNDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean equalsWithEps(final Object o, final double eps) {
        // TODO Auto-generated method stub
        return false;
    }

    public INDArray unsafeDuplication() {
        // TODO Auto-generated method stub
        return null;
    }

}
