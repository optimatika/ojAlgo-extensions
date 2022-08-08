/*
 * Copyright 1997-2022 Optimatika
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

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.ojalgo.data.domain.finance.series.DataSource;
import org.ojalgo.data.domain.finance.series.DatePrice;
import org.ojalgo.data.domain.finance.series.FinanceData;
import org.ojalgo.data.domain.finance.series.YahooParser;
import org.ojalgo.data.domain.finance.series.YahooParser.Data;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.series.BasicSeries;
import org.ojalgo.series.SimpleSeries;
import org.ojalgo.type.CalendarDate;
import org.ojalgo.type.CalendarDateUnit;
import org.ojalgo.type.PrimitiveNumber;
import org.ojalgo.type.keyvalue.KeyValue;

import yahoofinance.YahooFinance;
import yahoofinance.histquotes.Interval;

/**
 * <ul>
 * <li>https://financequotes-api.com
 * <li>https://github.com/sstrickx/yahoofinance-api
 * </ul>
 *
 * @author apete
 */
public final class YahooFinanceDataSource implements FinanceData<YahooParser.Data> {

    public static YahooFinanceDataSource of(final String symbol, final CalendarDateUnit resolution) {
        return new YahooFinanceDataSource(symbol, resolution);
    }

    private final CalendarDateUnit myResolution;
    private final String mySymbol;

    YahooFinanceDataSource(final String symbol, final CalendarDateUnit resolution) {

        super();

        mySymbol = symbol;
        myResolution = resolution;
    }

    @Override
    public KeyValue<String, List<Data>> getHistoricalData() {
        return KeyValue.of(this.getSymbol(), this.getHistoricalPrices());
    }

    @Override
    public List<YahooParser.Data> getHistoricalPrices() {

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
            return YahooFinance.get(mySymbol, from, to, interval).getHistory().stream()
                    .map(hq -> new YahooParser.Data(CalendarDate.toLocalDate(hq.getDate()), hq.getOpen() != null ? hq.getOpen().doubleValue() : Double.NaN,
                            hq.getHigh() != null ? hq.getHigh().doubleValue() : Double.NaN, hq.getLow() != null ? hq.getLow().doubleValue() : Double.NaN,
                            hq.getClose() != null ? hq.getClose().doubleValue() : Double.NaN,
                            hq.getAdjClose() != null ? hq.getAdjClose().doubleValue() : Double.NaN,
                            hq.getVolume() != null ? hq.getVolume().doubleValue() : Double.NaN))
                    .sorted().collect(Collectors.toList());
        } catch (Exception cause) {
            BasicLogger.error("Fetch problem for symbol {} & resolution {} using {}", mySymbol, myResolution, this.getClass());
            BasicLogger.error(cause);
            return Collections.emptyList();
        }
    }

    @Override
    public BasicSeries<LocalDate, PrimitiveNumber> getPriceSeries() {

        BasicSeries<LocalDate, PrimitiveNumber> retVal = new SimpleSeries<>();

        LocalDate adjusted;
        for (DatePrice datePrice : this.getHistoricalPrices()) {
            switch (myResolution) {
            case MONTH:
                adjusted = DataSource.LAST_DAY_OF_MONTH.apply(datePrice.date);
                break;
            case WEEK:
                adjusted = DataSource.FRIDAY_OF_WEEK.apply(datePrice.date);
                break;
            default:
                adjusted = datePrice.date;
                break;
            }
            retVal.put(adjusted, datePrice.getPrice());
        }

        return retVal;
    }

    @Override
    public String getSymbol() {
        return mySymbol;
    }

}
