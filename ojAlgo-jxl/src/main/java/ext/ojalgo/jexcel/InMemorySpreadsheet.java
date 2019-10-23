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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.ojalgo.matrix.Primitive64Matrix;
import org.ojalgo.scalar.Scalar;
import org.ojalgo.type.TypeUtils;

import ext.ojalgo.jexcel.database.Column;
import ext.ojalgo.jexcel.database.Table;
import jxl.BooleanCell;
import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.LabelCell;
import jxl.NumberCell;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.biff.DisplayFormat;
import jxl.format.CellFormat;
import jxl.read.biff.BiffException;
import jxl.write.NumberFormat;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class InMemorySpreadsheet implements Spreadsheet {

    private static final int FIRST = 0;
    private static final int LAST = Integer.MAX_VALUE;

    private int myColumn;
    private int myRow;
    private WritableSheet mySheet;
    private final ByteArrayOutputStream myStream;
    private final WritableWorkbook myWorkbook;

    public InMemorySpreadsheet() {

        super();

        myStream = new ByteArrayOutputStream();

        WritableWorkbook tmpWorkbook;
        try {
            tmpWorkbook = Workbook.createWorkbook(myStream, new WorkbookSettings());
        } catch (final IOException anException) {
            tmpWorkbook = null;
        }
        myWorkbook = tmpWorkbook;
    }

    public InMemorySpreadsheet(final InputStream aStream) {

        super();

        myStream = new ByteArrayOutputStream();

        WritableWorkbook tmpWorkbook;
        try {
            tmpWorkbook = Workbook.createWorkbook(myStream, Workbook.getWorkbook(aStream));
        } catch (final BiffException anException) {
            tmpWorkbook = null;
            anException.printStackTrace();
        } catch (final IOException anException) {
            tmpWorkbook = null;
            anException.printStackTrace();
        }
        myWorkbook = tmpWorkbook;
    }

    InMemorySpreadsheet(final Workbook aWorkbook) {

        super();

        myStream = new ByteArrayOutputStream();

        WritableWorkbook tmpWorkbook;
        try {
            tmpWorkbook = Workbook.createWorkbook(myStream, aWorkbook, new WorkbookSettings());
        } catch (final IOException anException) {
            tmpWorkbook = null;
        }

        myWorkbook = tmpWorkbook;
    }

    public void activateSheet(final String aName) {

        mySheet = myWorkbook.getSheet(aName);
        if (mySheet == null) {
            mySheet = myWorkbook.createSheet(aName, LAST);
        }

        myColumn = FIRST;
        myRow = FIRST;
    }

    public Boolean getBooleanCellValue() {

        final Cell tmpCell = this.getCell();
        final CellType tmpType = tmpCell.getType();

        if (tmpType == CellType.BOOLEAN) {
            return ((BooleanCell) tmpCell).getValue();
        } else {
            return null;
        }
    }

    public Object getCellValue() {

        final Cell tmpCell = this.getCell();
        final CellType tmpType = tmpCell.getType();

        if (tmpType == CellType.BOOLEAN) {
            return this.getBooleanCellValue();
        } else if (tmpType == CellType.DATE) {
            return this.getDateCellValue();
        } else if (tmpType == CellType.NUMBER) {
            return this.getNumberCellValue();
        } else if (tmpType == CellType.LABEL) {
            return this.getStringCellValue();
        } else {
            return null;
        }
    }

    public Date getDateCellValue() {

        final Cell tmpCell = this.getCell();
        final CellType tmpType = tmpCell.getType();

        if (tmpType == CellType.DATE) {
            return ((DateCell) tmpCell).getDate();
        } else {
            return null;
        }
    }

    public Primitive64Matrix getMatrixSheetValue() {

        this.goHome();
        while (this.getCell() instanceof NumberCell) {
            this.goToNextColumn();
        }
        final int tmpColDim = myColumn;

        this.goHome();
        while (this.getCell() instanceof NumberCell) {
            this.goToNextRow();
        }
        final int tmpRowDim = myRow;

        final double[][] retVal = new double[tmpRowDim][tmpColDim];

        this.goHome();
        for (int i = FIRST; i < tmpRowDim; i++) {
            for (int j = FIRST; j < tmpColDim; j++) {
                retVal[i][j] = Scalar.doubleValue(this.getNumberCellValue());
                this.goToNextColumn();
            }
            this.goToFirstColumnOnNextRow();
        }

        return Primitive64Matrix.FACTORY.rows(retVal);
    }

    public Comparable<?> getNumberCellValue() {

        final Cell tmpCell = this.getCell();
        final CellType tmpType = tmpCell.getType();

        if (tmpType == CellType.NUMBER) {
            return ((NumberCell) tmpCell).getValue();
        } else {
            return null;
        }
    }

    public String[] getSheetNames() {
        return myWorkbook.getSheetNames();
    }

    public String getStringCellValue() {

        final Cell tmpCell = this.getCell();
        final CellType tmpType = tmpCell.getType();

        if (tmpType == CellType.LABEL) {
            return ((LabelCell) tmpCell).getString();
        } else {
            return null;
        }
    }

    public void goHome() {
        myColumn = FIRST;
        myRow = FIRST;
    }

    public void goTo(final int aColumn, final int aRow) {
        myColumn = aColumn;
        myRow = aRow;
    }

    public void goToFirstColumnOnNextRow() {
        myColumn = FIRST;
        myRow++;
    }

    public void goToFirstRowInNextColumn() {
        myColumn++;
        myRow = FIRST;
    }

    public void goToNextColumn() {
        myColumn++;
    }

    public void goToNextRow() {
        myRow++;
    }

    public void setBooleanCellValue(final Boolean aCellValue) {
        if (aCellValue != null) {
            this.setCell(new jxl.write.Boolean(myColumn, myRow, aCellValue));
        }
    }

    public void setBooleanColumnValues(final List<Boolean> someColumnValues) {
        for (final Boolean tmpBoolean : someColumnValues) {
            this.setCell(new jxl.write.Boolean(myColumn, myRow, tmpBoolean));
            this.goToNextRow();
        }
    }

    public void setBooleanRowValues(final List<Boolean> someRowValues) {
        for (final Boolean tmpBoolean : someRowValues) {
            this.setCell(new jxl.write.Boolean(myColumn, myRow, tmpBoolean));
            this.goToNextColumn();
        }
    }

    public void setDateCellValue(final Date aCellValue) {
        if (aCellValue != null) {
            this.setCell(new jxl.write.DateTime(myColumn, myRow, aCellValue));
        }
    }

    public void setDateColumnValues(final List<Date> someColumnValues) {
        for (final Date tmpDate : someColumnValues) {
            this.setCell(new jxl.write.DateTime(myColumn, myRow, tmpDate));
            this.goToNextRow();
        }
    }

    public void setDateRowValues(final List<Date> someRowValues) {
        for (final Date tmpDate : someRowValues) {
            this.setCell(new jxl.write.DateTime(myColumn, myRow, tmpDate));
            this.goToNextColumn();
        }
    }

    public void setMatrixSheetValue(final Primitive64Matrix aSheetValue) {

        this.goHome();

        final int tmpRowDim = (int) aSheetValue.countRows();
        final int tmpColDim = (int) aSheetValue.countColumns();
        for (int i = FIRST; i < tmpRowDim; i++) {
            for (int j = FIRST; j < tmpColDim; j++) {
                final int row = i;
                final int col = j;
                this.setNumberCellValue(TypeUtils.toBigDecimal(aSheetValue.get(row, col)));
                this.goToNextColumn();
            }
            this.goToFirstColumnOnNextRow();
        }
    }

    public void setNumberCellValue(final Comparable<?> aCellValue) {
        this.setNumberCellValue(aCellValue, null);
    }

    public void setNumberCellValue(final Comparable<?> aCellValue, final String aPattern) {

        if (aCellValue != null) {

            if (aPattern != null) {

                final DisplayFormat tmpDisplayFormat = new NumberFormat(aPattern);
                final CellFormat tmpCellFormat = new WritableCellFormat(tmpDisplayFormat);

                this.setCell(new jxl.write.Number(myColumn, myRow, Scalar.doubleValue(aCellValue), tmpCellFormat));

            } else {

                this.setCell(new jxl.write.Number(myColumn, myRow, Scalar.doubleValue(aCellValue)));
            }
        }
    }

    public void setNumberColumnValues(final List<Comparable<?>> someColumnValues) {
        for (final Comparable<?> tmpNumber : someColumnValues) {
            this.setCell(new jxl.write.Number(myColumn, myRow, Scalar.doubleValue(tmpNumber)));
            this.goToNextRow();
        }
    }

    public void setNumberRowValues(final List<Comparable<?>> someRowValues) {
        for (final Comparable<?> tmpNumber : someRowValues) {
            this.setCell(new jxl.write.Number(myColumn, myRow, Scalar.doubleValue(tmpNumber)));
            this.goToNextColumn();
        }
    }

    public void setStringCellValue(final String aCellValue) {
        if (aCellValue != null) {
            this.setCell(new jxl.write.Label(myColumn, myRow, aCellValue));
        }
    }

    public void setStringColumnValues(final List<String> someColumnValues) {
        for (final String tmpString : someColumnValues) {
            this.setCell(new jxl.write.Label(myColumn, myRow, tmpString));
            this.goToNextRow();
        }
    }

    public void setStringRowValues(final List<String> someRowValues) {
        for (final String tmpString : someRowValues) {
            this.setCell(new jxl.write.Label(myColumn, myRow, tmpString));
            this.goToNextColumn();
        }
    }

    public void setTableSheetValue(final Table aSheetValue) {

        this.goHome();

        final List<Column<Object>> tmpColumns = aSheetValue.getColumns();
        for (final Column<Object> tmpColumn : tmpColumns) {

            this.setStringCellValue(tmpColumn.getName());
            this.goToNextRow();

            final List<Object> tmpData = tmpColumn.getData();
            for (final Object tmpCellValue : tmpData) {

                tmpColumn.setCellValue(this, tmpCellValue);
                this.goToNextRow();
            }

            this.goToFirstRowInNextColumn();
        }
    }

    public byte[] toByteArray() {

        try {
            myWorkbook.write();
            myWorkbook.close();
        } catch (final IOException anException) {
            // TODO Auto-generated catch block
            anException.printStackTrace();
        } catch (final WriteException anException) {
            // TODO Auto-generated catch block
            anException.printStackTrace();
        }

        return myStream.toByteArray();
    }

    @Override
    public String toString() {

        try {
            myWorkbook.write();
            myWorkbook.close();
        } catch (final IOException anException) {
            // TODO Auto-generated catch block
            anException.printStackTrace();
        } catch (final WriteException anException) {
            // TODO Auto-generated catch block
            anException.printStackTrace();
        }

        return myStream.toString();
    }

    public void writeToFile(final File aFile) {

        try {

            myWorkbook.write();
            myWorkbook.close();

            final FileOutputStream tmpFileStream = new FileOutputStream(aFile);

            myStream.writeTo(tmpFileStream);

            tmpFileStream.close();

        } catch (final IOException anException) {
            // TODO Auto-generated catch block
            anException.printStackTrace();
        } catch (final WriteException anException) {
            // TODO Auto-generated catch block
            anException.printStackTrace();
        }
    }

    private Cell getCell() {
        return mySheet.getCell(myColumn, myRow);
    }

    private void setCell(final WritableCell aCell) {
        try {
            mySheet.addCell(aCell);
        } catch (final RowsExceededException anException) {
            // TODO Auto-generated catch block
            anException.printStackTrace();
        } catch (final WriteException anException) {
            // TODO Auto-generated catch block
            anException.printStackTrace();
        }
    }
}
