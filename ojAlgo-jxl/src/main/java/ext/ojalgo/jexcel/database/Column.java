/*
 * Copyright 1997-2019 Optimatika
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
package ext.ojalgo.jexcel.database;

import java.util.ArrayList;
import java.util.List;

import org.ojalgo.type.context.TypeContext;

import ext.ojalgo.jexcel.Spreadsheet;

public abstract class Column<T> {

    private final String myName;
    private final List<T> myData = new ArrayList<>();
    private final TypeContext<T> myTypeContext;

    public Column(final String aName, final TypeContext<T> aContext) {

        super();

        myName = aName;
        myTypeContext = aContext;
    }

    @SuppressWarnings("unused")
    private Column() {
        this(null, null);
    }

    public void add(final int index, final T anObject) {
        myData.add(index, anObject);
    }

    public boolean add(final T anObject) {
        return myData.add(anObject);
    }

    public T get(final int index) {
        return myData.get(index);
    }

    public abstract T getCellValue(Spreadsheet aSheet);

    public List<T> getData() {
        return myData;
    }

    public String getName() {
        return myName;
    }

    public TypeContext<T> getTypeContext() {
        return myTypeContext;
    }

    public int indexOf(final T anObject) {
        return myData.indexOf(anObject);
    }

    public T remove(final int index) {
        return myData.remove(index);
    }

    public T set(final int index, final T anObject) {
        return myData.set(index, anObject);
    }

    public abstract void setCellValue(Spreadsheet aSheet, T aCellValue);

}
