package org.ojalgo.linalg.jvm;

import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.ojalgo.array.ArrayAnyD;
import org.ojalgo.array.ArrayAnyD.Factory;
import org.ojalgo.array.ComplexArray;
import org.ojalgo.scalar.ComplexNumber;

public final class ComplexArrayND extends ArrayND<ComplexNumber> implements IComplexNDArray {

    public ComplexArrayND(long count) {
        super(ComplexArray.FACTORY, count);
    }

    public ComplexArrayND(long rows, long columns) {
        super(ComplexArray.FACTORY, rows, columns);
    }

    public ComplexArrayND(long[] structure) {
        super(ComplexArray.FACTORY, structure);
    }

    ComplexArrayND(Factory<ComplexNumber> factory, ArrayAnyD<ComplexNumber> delegate) {
        super(factory, delegate);
    }

    public IComplexNDArray add(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray addi(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray assign(IComplexNDArray arr) {
        // TODO Auto-generated method stub
        return null;
    }

    public void assign(IComplexNumber aDouble) {
        // TODO Auto-generated method stub

    }

    public int blasOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    public IComplexNDArray conj() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray conji() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray div(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray divi(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray eps(IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray epsi(IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray eq(IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray eqi(IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber getComplex(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber getComplex(int... indices) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber getComplex(int i, IComplexNumber result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber getComplex(int i, int j) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNumber getComplex(int i, int j, IComplexNumber result) {
        // TODO Auto-generated method stub
        return null;
    }

    public double getImag(int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    public INDArray getReal() {
        // TODO Auto-generated method stub
        return null;
    }

    public double getReal(int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    public IComplexNDArray gt(IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray gti(IComplexNumber other) {
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

    public IComplexNDArray lt(IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray lti(IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray mul(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray muli(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray neq(IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray neqi(IComplexNumber other) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(INDArrayIndex[] indices, IComplexNDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(INDArrayIndex[] indices, IComplexNumber element) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(int i, IComplexNDArray element) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(int i, int j, IComplexNumber complex) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(int[] indexes, double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray put(int[] indexes, float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putImag(int i, float v) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putImag(int rowIndex, int columnIndex, double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putImag(int rowIndex, int columnIndex, float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putImag(int[] indices, double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putImag(int[] indices, float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putReal(int i, float v) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putReal(int rowIndex, int columnIndex, double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putReal(int rowIndex, int columnIndex, float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putReal(int[] indices, double value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putReal(int[] indices, float value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putScalar(int i, IComplexNumber value) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putScalar(int j, int i, IComplexNumber conji) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray putScalar(int[] i, IComplexNumber complexNumber) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray putSlice(int slice, IComplexNDArray put) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rdiv(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rdivi(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public INDArray real() {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rsub(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray rsubi(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray sub(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

    public IComplexNDArray subi(IComplexNumber n, INDArray result) {
        // TODO Auto-generated method stub
        return null;
    }

}
