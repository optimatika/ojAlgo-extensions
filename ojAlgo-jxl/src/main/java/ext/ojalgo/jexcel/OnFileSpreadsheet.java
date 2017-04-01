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
import java.io.IOException;
import java.util.Date;
import java.util.List;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

import org.ojalgo.matrix.BasicMatrix;

import ext.ojalgo.jexcel.database.Table;


public class OnFileSpreadsheet implements Spreadsheet {

    private static final String BACKUP = ".backup";
    private Workbook myBackup = null;
    private InMemorySpreadsheet myCurrent = null;
    private final File myFile;

    public OnFileSpreadsheet(final File aFile) {

        super();

        myFile = aFile;

        this.readFromFile();

        this.revert();
    }

    @SuppressWarnings("unused")
    private OnFileSpreadsheet() {
        this(null);
    }

    public void activateSheet(final String aName) {
        myCurrent.activateSheet(aName);
    }

    public Boolean getBooleanCellValue() {
        return myCurrent.getBooleanCellValue();
    }

    public Date getDateCellValue() {
        return myCurrent.getDateCellValue();
    }

    public BasicMatrix getMatrixSheetValue() {
        return myCurrent.getMatrixSheetValue();
    }

    public Number getNumberCellValue() {
        return myCurrent.getNumberCellValue();
    }

    public String[] getSheetNames() {
        return myCurrent.getSheetNames();
    }

    public String getStringCellValue() {
        return myCurrent.getStringCellValue();
    }

    public void goHome() {
        myCurrent.goHome();
    }

    public void goTo(final int aColumn, final int aRow) {
        myCurrent.goTo(aColumn, aRow);
    }

    public void goToFirstColumnOnNextRow() {
        myCurrent.goToFirstColumnOnNextRow();
    }

    public void goToFirstRowInNextColumn() {
        myCurrent.goToFirstRowInNextColumn();
    }

    public void goToNextColumn() {
        myCurrent.goToNextColumn();
    }

    public void goToNextRow() {
        myCurrent.goToNextRow();
    }

    /**
     * Undo changes. Revert to last saved file.
     */
    public void revert() {
        if (myBackup != null) {
            myCurrent = new InMemorySpreadsheet(myBackup);
        } else {
            myCurrent = new InMemorySpreadsheet();
        }
    }

    public void setBooleanCellValue(final Boolean aCellValue) {
        myCurrent.setBooleanCellValue(aCellValue);
    }

    public void setBooleanColumnValues(final List<Boolean> someColumnValues) {
        myCurrent.setBooleanColumnValues(someColumnValues);
    }

    public void setBooleanRowValues(final List<Boolean> someRowValues) {
        myCurrent.setBooleanRowValues(someRowValues);
    }

    public void setDateCellValue(final Date aCellValue) {
        myCurrent.setDateCellValue(aCellValue);
    }

    public void setDateColumnValues(final List<Date> someColumnValues) {
        myCurrent.setDateColumnValues(someColumnValues);
    }

    public void setDateRowValues(final List<Date> someRowValues) {
        myCurrent.setDateRowValues(someRowValues);
    }

    public void setMatrixSheetValue(final BasicMatrix aSheetValue) {
        myCurrent.setMatrixSheetValue(aSheetValue);
    }

    public void setNumberCellValue(final Number aCellValue) {
        myCurrent.setNumberCellValue(aCellValue);
    }

    public void setNumberCellValue(final Number aCellValue, final String aPattern) {
        myCurrent.setNumberCellValue(aCellValue, aPattern);
    }

    public void setNumberColumnValues(final List<Number> someColumnValues) {
        myCurrent.setNumberColumnValues(someColumnValues);
    }

    public void setNumberRowValues(final List<Number> someRowValues) {
        myCurrent.setNumberRowValues(someRowValues);
    }

    public void setStringCellValue(final String aCellValue) {
        myCurrent.setStringCellValue(aCellValue);
    }

    public void setStringColumnValues(final List<String> someColumnValues) {
        myCurrent.setStringColumnValues(someColumnValues);
    }

    public void setStringRowValues(final List<String> someRowValues) {
        myCurrent.setStringRowValues(someRowValues);
    }

    public void setTableSheetValue(final Table aSheetValue) {
        myCurrent.setTableSheetValue(aSheetValue);
    }

    public byte[] toByteArray() {
        return myCurrent.toByteArray();
    }

    public void writeToFile() {

        if (myBackup != null) {
            final File tmpBackupFile = new File(myFile.getPath() + BACKUP);
            final InMemorySpreadsheet tmpBackupCopy = new InMemorySpreadsheet(myBackup);
            tmpBackupFile.delete();
            tmpBackupCopy.writeToFile(tmpBackupFile);
        }

        myFile.delete();
        myCurrent.writeToFile(myFile);

        this.readFromFile();

        this.revert();
    }

    public void writeToFile(final File aFile) {
        myCurrent.writeToFile(aFile);
    }

    private void readFromFile() {
        try {
            if (myFile.canRead()) {
                myBackup = Workbook.getWorkbook(myFile, new WorkbookSettings());
            } else {
                myBackup = null;
            }
        } catch (final BiffException anException) {
            myBackup = null;
            anException.printStackTrace();
        } catch (final IOException anException) {
            myBackup = null;
            anException.printStackTrace();
        }
    }

    protected final File getFile() {
        return myFile;
    }

}
