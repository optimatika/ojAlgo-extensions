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

public final class Table {

    private final List<Column<Object>> myColumns = new ArrayList<>();

    public Table() {
        super();
    }

    public Table(final Column<Object>[] someColumns) {

        super();

        for (final Column<Object> tmpColumn : someColumns) {
            myColumns.add(tmpColumn);
        }
    }

    public void addRow(final int index, final List<?> aRow) {
        for (int tmpCol = 0; tmpCol < myColumns.size(); tmpCol++) {
            myColumns.get(tmpCol).add(index, aRow.get(tmpCol));
        }
    }

    public void addRow(final List<?> aRow) {
        for (int tmpCol = 0; tmpCol < myColumns.size(); tmpCol++) {
            myColumns.get(tmpCol).add(aRow.get(tmpCol));
        }
    }

    public List<String> getColumnNames() {
        final List<String> retVal = new ArrayList<>();
        for (int tmpCol = 0; tmpCol < myColumns.size(); tmpCol++) {
            retVal.add(tmpCol, myColumns.get(tmpCol).getName());
        }
        return retVal;
    }

    public List<Column<Object>> getColumns() {
        return myColumns;
    }

    public List<TypeContext<Object>> getColumnTypeContexts() {
        final List<TypeContext<Object>> retVal = new ArrayList<>();
        for (int tmpCol = 0; tmpCol < myColumns.size(); tmpCol++) {
            retVal.add(tmpCol, myColumns.get(tmpCol).getTypeContext());
        }
        return retVal;
    }

    public List<Object> getRow(final int index) {
        final List<Object> retVal = new ArrayList<>();
        for (int tmpCol = 0; tmpCol < myColumns.size(); tmpCol++) {
            retVal.add(tmpCol, myColumns.get(tmpCol).get(index));
        }
        return retVal;
    }

    public List<Object> removeRow(final int index) {
        final List<Object> retVal = new ArrayList<>();
        for (int tmpCol = 0; tmpCol < myColumns.size(); tmpCol++) {
            retVal.add(tmpCol, myColumns.get(tmpCol).remove(index));
        }
        return retVal;
    }

    public List<Object> setRow(final int index, final List<?> aRow) {
        final List<Object> retVal = new ArrayList<>();
        for (int tmpCol = 0; tmpCol < myColumns.size(); tmpCol++) {
            retVal.add(tmpCol, myColumns.get(tmpCol).set(index, aRow.get(tmpCol)));
        }
        return retVal;
    }

}
