/*
 * Copyright 1997-2020 Optimatika
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
package ext.ojalgo.jfree.chart;

import java.awt.Color;
import java.util.List;

import org.jfree.chart.plot.Plot;
import org.ojalgo.finance.portfolio.SimpleAsset;
import org.ojalgo.finance.portfolio.SimplePortfolio;
import org.ojalgo.finance.portfolio.simulator.PortfolioSimulator;
import org.ojalgo.function.constant.PrimitiveMath;
import org.ojalgo.random.LogNormal;
import org.ojalgo.random.SampleSet;
import org.ojalgo.random.process.GeometricBrownianMotion;
import org.ojalgo.random.process.RandomProcess;
import org.ojalgo.series.CalendarDateSeries;
import org.ojalgo.structure.Access2D;
import org.ojalgo.type.CalendarDate;
import org.ojalgo.type.CalendarDateDuration;
import org.ojalgo.type.CalendarDateUnit;
import org.ojalgo.type.ColourData;

/**
 * @author apete
 */
public class MonteCarloBuilder extends HistoryAndForecastBuilder {

    private final CalendarDateUnit myAssetDataUnit;
    private double myConfidence = 0.95;
    private final SimplePortfolio myPortfolio;
    private CalendarDateDuration myRange = new CalendarDateDuration(3, CalendarDateUnit.YEAR);
    private CalendarDateDuration myRebalance = new CalendarDateDuration(1, CalendarDateUnit.YEAR);
    private int myRealisations = 9;
    private CalendarDateUnit myResolution = CalendarDateUnit.WEEK;

    public MonteCarloBuilder(final Access2D<?> aCorrelationsMatrix, final List<SimpleAsset> someAssets) {
        this(aCorrelationsMatrix, someAssets, CalendarDateUnit.YEAR);
    }

    public MonteCarloBuilder(final Access2D<?> aCorrelationsMatrix, final List<SimpleAsset> someAssets, final CalendarDateUnit assetDataUnit) {

        super();

        myPortfolio = new SimplePortfolio(aCorrelationsMatrix, someAssets);
        myAssetDataUnit = assetDataUnit;
        final CalendarDateSeriesCollection r = this.legend(false).deviationRenderer(true);
        r.range.logarithmic(true);

    }

    private MonteCarloBuilder() {
        this(null, null);
    }

    public final MonteCarloBuilder confidence(final double aValue) {
        myConfidence = aValue;
        return this;
    }

    public final MonteCarloBuilder range(final CalendarDateDuration aValue) {
        myRange = aValue;
        return this;
    }

    public final MonteCarloBuilder rebalance(final CalendarDateDuration aValue) {
        myRebalance = aValue;
        return this;
    }

    public final MonteCarloBuilder resolution(final CalendarDateUnit aValue) {
        myResolution = aValue;
        return this;
    }

    public final MonteCarloBuilder scenarios(final int aValue) {
        myRealisations = aValue;
        return this;
    }

    @Override
    protected Plot makePlot(final JFreeChartBuilder.PlotParameters parameters) {

        final GeometricBrownianMotion tmpForecaster = myPortfolio.forecast();
        final double tmpInitialValue = tmpForecaster.getValue();

        final int tmpSteps = myRange.convertTo(myResolution).intValue();
        final double tmpStepSize = myAssetDataUnit.convert(myResolution);
        final int tmpInterval = myRebalance.convertTo(myResolution).intValue();

        final LogNormal[] tmpDistributions = new LogNormal[tmpSteps];
        for (int s = 0; s < tmpSteps; s++) {
            tmpDistributions[s] = tmpForecaster.getDistribution(tmpStepSize * (PrimitiveMath.ONE + s));
        }

        final RandomProcess.SimulationResults tmpSamples;
        if (tmpInterval > 0) {
            final PortfolioSimulator tmpSimulator = myPortfolio.getSimulator();
            tmpSamples = tmpSimulator.simulate(myRealisations, tmpSteps, tmpStepSize, tmpInterval);
        } else {
            tmpSamples = tmpForecaster.simulate(myRealisations, tmpSteps, tmpStepSize);
        }

        CalendarDate tmpDate = CalendarDate.make(myResolution);

        final CalendarDateSeries<NumberWithRange> tmpDistributionConfidenceSeries = new CalendarDateSeries<NumberWithRange>(myResolution)
                .name("Distribution Confidence").colour(new ColourData(Color.LIGHT_GRAY.getRGB()));
        tmpDistributionConfidenceSeries.put(tmpDate, new NumberWithRange(tmpInitialValue));
        final CalendarDateSeries<Double>[] tmpMonteCarloScenarioSeries = (CalendarDateSeries<Double>[]) new CalendarDateSeries<?>[myRealisations];
        for (int r = 0; r < myRealisations; r++) {
            tmpMonteCarloScenarioSeries[r] = new CalendarDateSeries<Double>(myResolution).name("Scenario-" + r).colour(ColourData.random());
            tmpMonteCarloScenarioSeries[r].put(tmpDate, tmpInitialValue);
        }

        for (int s = 0; s < tmpSteps; s++) {

            tmpDate = tmpDate.step(myResolution);

            final SampleSet tmpSampleSet = tmpSamples.getSampleSet(s);
            final LogNormal tmpLogNormal = tmpDistributions[s];

            tmpDistributionConfidenceSeries.put(tmpDate, new NumberWithRange(tmpLogNormal, myConfidence));

            for (int r = 0; r < myRealisations; r++) {
                tmpMonteCarloScenarioSeries[r].put(tmpDate, tmpSampleSet.get(r));
            }
        }

        this.add(tmpDistributionConfidenceSeries);
        for (int r = 0; r < myRealisations; r++) {
            this.add(tmpMonteCarloScenarioSeries[r]);
        }

        return super.makePlot(parameters);
    }

}
