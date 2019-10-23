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
package ext.ojalgo.jexcel;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.ojalgo.matrix.Primitive64Matrix;

import ext.ojalgo.jexcel.database.Table;

/**
 * A <code>Spreadsheet</code> actually refers to a collection of (spread)sheets. Just like a Workbook...
 *
 * @author apete
 */
public interface Spreadsheet {

    void activateSheet(String aName);

    Boolean getBooleanCellValue();

    Date getDateCellValue();

    Primitive64Matrix getMatrixSheetValue();

    Comparable<?> getNumberCellValue();

    String[] getSheetNames();

    String getStringCellValue();

    void goHome();

    void goTo(int aColumn, int aRow);

    void goToFirstColumnOnNextRow();

    void goToFirstRowInNextColumn();

    void goToNextColumn();

    void goToNextRow();

    void setBooleanCellValue(Boolean aCellValue);

    void setBooleanColumnValues(List<Boolean> someColumnValues);

    void setBooleanRowValues(List<Boolean> someRowValues);

    void setDateCellValue(Date aCellValue);

    void setDateColumnValues(List<Date> someColumnValues);

    void setDateRowValues(List<Date> someRowValues);

    void setMatrixSheetValue(Primitive64Matrix aSheetValue);

    void setNumberCellValue(Comparable<?> aCellValue);

    void setNumberCellValue(Comparable<?> aCellValue, String aPattern);

    void setNumberColumnValues(List<Comparable<?>> someColumnValues);

    void setNumberRowValues(List<Comparable<?>> someRowValues);

    void setStringCellValue(String aCellValue);

    void setStringColumnValues(List<String> someColumnValues);

    void setStringRowValues(List<String> someRowValues);

    void setTableSheetValue(Table aSheetValue);

    byte[] toByteArray();

    void writeToFile(File aFile);

}
