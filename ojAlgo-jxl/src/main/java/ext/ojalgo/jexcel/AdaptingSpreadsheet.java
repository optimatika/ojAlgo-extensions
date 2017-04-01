/*
 * Copyright 1997-2014 Optimatika (www.optimatika.se)
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

import org.ojalgo.matrix.BasicMatrix;

import ext.ojalgo.jexcel.database.Table;


public abstract class AdaptingSpreadsheet<T> implements Spreadsheet {

    private final Spreadsheet mySpreadsheet;

    public AdaptingSpreadsheet(final Spreadsheet aSpreadsheet) {

        super();

        mySpreadsheet = aSpreadsheet;
    }

    @SuppressWarnings("unused")
    private AdaptingSpreadsheet() {
        this(null);
    }

    public void activateSheet(final String aName) {
        mySpreadsheet.activateSheet(aName);
    }

    public abstract T getAdaptedObject();

    public Boolean getBooleanCellValue() {
        return mySpreadsheet.getBooleanCellValue();
    }

    public Date getDateCellValue() {
        return mySpreadsheet.getDateCellValue();
    }

    public BasicMatrix getMatrixSheetValue() {
        return mySpreadsheet.getMatrixSheetValue();
    }

    public Number getNumberCellValue() {
        return mySpreadsheet.getNumberCellValue();
    }

    public String[] getSheetNames() {
        return mySpreadsheet.getSheetNames();
    }

    public final Spreadsheet getSpreadsheet() {
        return mySpreadsheet;
    }

    public String getStringCellValue() {
        return mySpreadsheet.getStringCellValue();
    }

    public void goHome() {
        mySpreadsheet.goHome();
    }

    public void goTo(final int aColumn, final int aRow) {
        mySpreadsheet.goTo(aColumn, aRow);
    }

    public void goToFirstColumnOnNextRow() {
        mySpreadsheet.goToFirstColumnOnNextRow();
    }

    public void goToFirstRowInNextColumn() {
        mySpreadsheet.goToFirstRowInNextColumn();
    }

    public void goToNextColumn() {
        mySpreadsheet.goToNextColumn();
    }

    public void goToNextRow() {
        mySpreadsheet.goToNextRow();
    }

    public abstract void setAdaptedObject(T anObj);

    public void setBooleanCellValue(final Boolean aCellValue) {
        mySpreadsheet.setBooleanCellValue(aCellValue);
    }

    public void setBooleanColumnValues(final List<Boolean> someColumnValues) {
        mySpreadsheet.setBooleanColumnValues(someColumnValues);
    }

    public void setBooleanRowValues(final List<Boolean> someRowValues) {
        mySpreadsheet.setBooleanRowValues(someRowValues);
    }

    public void setDateCellValue(final Date aCellValue) {
        mySpreadsheet.setDateCellValue(aCellValue);
    }

    public void setDateColumnValues(final List<Date> someColumnValues) {
        mySpreadsheet.setDateColumnValues(someColumnValues);
    }

    public void setDateRowValues(final List<Date> someRowValues) {
        mySpreadsheet.setDateRowValues(someRowValues);
    }

    public void setMatrixSheetValue(final BasicMatrix aSheetValue) {
        mySpreadsheet.setMatrixSheetValue(aSheetValue);
    }

    public void setNumberCellValue(final Number aCellValue) {
        mySpreadsheet.setNumberCellValue(aCellValue);
    }

    public void setNumberCellValue(final Number aCellValue, final String aPattern) {
        mySpreadsheet.setNumberCellValue(aCellValue, aPattern);
    }

    public void setNumberColumnValues(final List<Number> someColumnValues) {
        mySpreadsheet.setNumberColumnValues(someColumnValues);
    }

    public void setNumberRowValues(final List<Number> someRowValues) {
        mySpreadsheet.setNumberRowValues(someRowValues);
    }

    public void setStringCellValue(final String aCellValue) {
        mySpreadsheet.setStringCellValue(aCellValue);
    }

    public void setStringColumnValues(final List<String> someColumnValues) {
        mySpreadsheet.setStringColumnValues(someColumnValues);
    }

    public void setStringRowValues(final List<String> someRowValues) {
        mySpreadsheet.setStringRowValues(someRowValues);
    }

    public void setTableSheetValue(final Table aSheetValue) {
        mySpreadsheet.setTableSheetValue(aSheetValue);
    }

    public byte[] toByteArray() {
        return mySpreadsheet.toByteArray();
    }

    public void writeToFile(final File aFile) {
        mySpreadsheet.writeToFile(aFile);
    }

}
