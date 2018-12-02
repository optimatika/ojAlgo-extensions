/*
 * Copyright 1997-2018 Optimatika
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
package org.ojAlgo.yahoofinance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.ojalgo.array.DenseArray;
import org.ojalgo.finance.data.DatePrice;
import org.ojalgo.finance.data.FinanceData;
import org.ojalgo.finance.data.parser.YahooParser;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.series.BasicSeries;
import org.ojalgo.series.CalendarDateSeries;
import org.ojalgo.type.CalendarDate;
import org.ojalgo.type.CalendarDateUnit;

import yahoofinance.YahooFinance;
import yahoofinance.histquotes.Interval;

public final class YahooFinanceDataSource implements FinanceData {

    private static final TemporalAdjuster FRIDAY = TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY);
    private static final TemporalAdjuster LAST_DAY_OF_MONTH = TemporalAdjusters.lastDayOfMonth();

    public static YahooFinanceDataSource of(String symbol, CalendarDateUnit resolution) {
        return new YahooFinanceDataSource(symbol, resolution);
    }

    private final String mySymbol;
    private final CalendarDateUnit myResolution;

    YahooFinanceDataSource(String symbol, CalendarDateUnit resolution) {

        super();

        mySymbol = symbol;
        myResolution = resolution;
    }

    public List<DatePrice> getHistoricalPrices() {

        Calendar from = Calendar.getInstance();
        from.add(Calendar.YEAR, -30);

        Calendar to = Calendar.getInstance();

        Interval interval = Interval.DAILY;
        switch (myResolution) {
        case WEEK:
            interval = Interval.WEEKLY;
            break;
        case MONTH:
            interval = Interval.MONTHLY;
            break;
        default:
            interval = Interval.DAILY;
            break;
        }

        try {
            return YahooFinance.get(mySymbol, from, to, interval).getHistory().stream().map(hq -> {
                YahooParser.Data dp = new YahooParser.Data(CalendarDate.toLocalDate(hq.getDate()));
                dp.adjustedClose = hq.getAdjClose() != null ? hq.getAdjClose().doubleValue() : Double.NaN;
                dp.close = hq.getClose() != null ? hq.getClose().doubleValue() : Double.NaN;
                dp.high = hq.getHigh() != null ? hq.getHigh().doubleValue() : Double.NaN;
                dp.low = hq.getLow() != null ? hq.getLow().doubleValue() : Double.NaN;
                dp.open = hq.getOpen() != null ? hq.getOpen().doubleValue() : Double.NaN;
                dp.volume = hq.getVolume() != null ? hq.getVolume().doubleValue() : Double.NaN;
                return dp;
            }).sorted().collect(Collectors.toList());
        } catch (final Exception exception) {
            exception.printStackTrace();
            BasicLogger.error("Fetch problem for {}!", this.getClass().getSimpleName());
            BasicLogger.error("Symbol & Resolution: {} & {}", mySymbol, myResolution);
            return Collections.emptyList();
        }
    }

    public CalendarDateSeries<Double> getPriceSeries() {
        return this.getPriceSeries(myResolution, LocalTime.NOON, ZoneOffset.UTC);
    }

    public CalendarDateSeries<Double> getPriceSeries(CalendarDateUnit resolution) {
        return this.getPriceSeries(resolution, LocalTime.NOON, ZoneOffset.UTC);
    }

    public CalendarDateSeries<Double> getPriceSeries(CalendarDateUnit resolution, LocalTime time, ZoneId zoneId) {

        CalendarDateSeries<Double> retVal = new CalendarDateSeries<>(resolution);

        for (DatePrice datePrice : this.getHistoricalPrices()) {
            retVal.put(CalendarDate.valueOf(datePrice.key.atTime(time).atZone(zoneId)), datePrice.getPrice());
        }

        return retVal;
    }

    public BasicSeries.NaturallySequenced<LocalDate, Double> getPriceSeries(DenseArray.Factory<Double> denseArrayFactory) {

        BasicSeries.NaturallySequenced<LocalDate, Double> retVal = BasicSeries.LOCAL_DATE.build(denseArrayFactory);

        LocalDate adjusted;
        for (DatePrice datePrice : this.getHistoricalPrices()) {
            switch (myResolution) {
            case MONTH:
                adjusted = (LocalDate) LAST_DAY_OF_MONTH.adjustInto(datePrice.key);
                break;
            case WEEK:
                adjusted = (LocalDate) FRIDAY.adjustInto(datePrice.key);
                break;
            default:
                adjusted = datePrice.key;
                break;
            }
            retVal.put(adjusted, datePrice.getPrice());
        }

        return retVal;
    }

    public CalendarDateSeries<Double> getPriceSeries(LocalTime time, ZoneId zoneId) {
        return this.getPriceSeries(myResolution, time, zoneId);
    }

    public String getSymbol() {
        return mySymbol;
    }

}
