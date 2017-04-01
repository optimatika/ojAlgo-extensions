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
package ext.ojalgo.jexcel.finance;

import org.ojalgo.finance.portfolio.BlackLittermanModel;

import ext.ojalgo.jexcel.AdaptingSpreadsheet;
import ext.ojalgo.jexcel.Spreadsheet;


public class BlackLittermanSpreadsheet extends AdaptingSpreadsheet<BlackLittermanModel> {

    private static final String COVARIANCES = "Covariances";
    private static final String INSTRUMENTS = "Instruments";
    private static final String MARKET_RETURNS = "MarketReturns";
    private static final String MARKET_WEIGHTS = "MarketWeights";
    private static final String MODIFIED_RETURNS = "ModifiedReturns";
    private static final String MODIFIED_WEIGHTS = "ModifiedWeights";
    private static final String NORMALISED_WEIGHTS = "NormalisedWeights";
    private static final String OPTIMISED_WEIGHTS = "OptimisedWeights";
    private static final String RISK_AVERSION = "RiskAversion";
    private static final String VIEW_CONFIDENCES = "ViewConfidences";
    private static final String VIEW_RETURNS = "ViewReturns";
    private static final String VIEWS = "Views";
    private static final String WEIGHT_ON_VIEWS = "WeightOnViews";

    public BlackLittermanSpreadsheet(final Spreadsheet aSpreadsheet) {
        super(aSpreadsheet);
    }

    @Override
    public BlackLittermanModel getAdaptedObject() {

        // TODO Copy/parse to BlackLittermanModel

        return null;
    }

    @Override
    public void setAdaptedObject(final BlackLittermanModel anObj) {

        this.activateSheet(INSTRUMENTS);
        for (final String tmpInstrument : anObj.getSymbols()) {
            this.setStringCellValue(tmpInstrument);
            this.goToNextRow();
        }

        this.activateSheet(COVARIANCES);
        this.setMatrixSheetValue(anObj.getCovariances());

        this.activateSheet(VIEWS);
        //this.setMatrixSheetValue(anObj.getViewPortfolios());

        this.activateSheet(VIEW_RETURNS);
        //this.setMatrixSheetValue(anObj.getViewReturns());

        this.activateSheet(VIEW_CONFIDENCES);
        //this.setMatrixSheetValue(anObj.getViewConfidences());

        this.activateSheet(RISK_AVERSION);
        this.setNumberCellValue(anObj.getRiskAversion().getNumber());

        this.activateSheet(WEIGHT_ON_VIEWS);
        this.setNumberCellValue(anObj.getConfidence().getNumber());

        this.activateSheet(MARKET_RETURNS);
        //this.setMatrixSheetValue(anObj.getOriginalReturns());

        this.activateSheet(MODIFIED_RETURNS);
        this.setMatrixSheetValue(anObj.getAssetReturns());

        this.activateSheet(MARKET_WEIGHTS);
        //this.setMatrixSheetValue(anObj.getOriginalWeights());

        this.activateSheet(MODIFIED_WEIGHTS);
        //this.setMatrixSheetValue(anObj.getInstrumentWeights());

        this.activateSheet(NORMALISED_WEIGHTS);
        //this.setMatrixSheetValue(anObj.getNormalisedWeights());

        this.activateSheet(OPTIMISED_WEIGHTS);
        //this.setMatrixSheetValue(anObj.getOptimisedPortfolio().getInstrumentWeights());
    }

}
