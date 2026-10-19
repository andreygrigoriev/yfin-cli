package io.github.andreygrigoriev.yfin.cli.spec;

import io.github.andreygrigoriev.yfin.FastInfo;
import io.github.andreygrigoriev.yfin.cli.output.Kind;
import io.github.andreygrigoriev.yfin.cli.output.Spec;
import io.github.andreygrigoriev.yfin.cli.output.Symbol;
import io.github.andreygrigoriev.yfin.model.Bar;
import io.github.andreygrigoriev.yfin.model.Calendar;
import io.github.andreygrigoriev.yfin.model.CapitalGain;
import io.github.andreygrigoriev.yfin.model.Dividend;
import io.github.andreygrigoriev.yfin.model.FundHolding;
import io.github.andreygrigoriev.yfin.model.FundsData;
import io.github.andreygrigoriev.yfin.model.SecFiling;
import io.github.andreygrigoriev.yfin.model.SharesCount;
import io.github.andreygrigoriev.yfin.model.Split;
import io.github.andreygrigoriev.yfin.model.Sustainability;
import io.github.andreygrigoriev.yfin.model.TickerInfo;
import io.github.andreygrigoriev.yfin.request.Interval;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/** Column specs of the library models the commands print. */
public final class Specs {
    /** Prices: two fraction digits in tables, full precision in CSV and JSON. */
    public static final Kind PRICE = Kind.decimal(2);

    /** Ratios and scores (P/E, beta, ESG): two fraction digits in tables, full precision elsewhere. */
    public static final Kind RATIO = Kind.decimal(2);

    /**
     * A {@link FastInfo} snapshot, as {@code quote} prints it. {@code change} and {@code change_pct}
     * compare the last price with the previous regular-session close.
     */
    public static final Spec<FastInfo> FAST_INFO = Spec.<FastInfo>builder()
            .col("last_price", FastInfo::lastPrice, PRICE)
            .col("change", Specs::change, PRICE)
            .col("change_pct", Specs::changePct, Kind.PERCENT)
            .col("previous_close", FastInfo::regularMarketPreviousClose, PRICE)
            .col("open", FastInfo::open, PRICE)
            .col("day_high", FastInfo::dayHigh, PRICE)
            .col("day_low", FastInfo::dayLow, PRICE)
            .col("volume", FastInfo::lastVolume, Kind.INTEGER)
            .col("market_cap", FastInfo::marketCap, Kind.INTEGER)
            .col("year_high", FastInfo::yearHigh, PRICE)
            .col("year_low", FastInfo::yearLow, PRICE)
            .col("currency", FastInfo::currency, Kind.TEXT)
            .col("exchange", FastInfo::exchange, Kind.TEXT)
            .build();

    /** Intraday price bars: the bar start as a timestamp in the exchange zone. */
    public static final Spec<Bar> BAR = bars("time", Bar::time, Kind.TIMESTAMP);

    /** Daily and longer price bars: the bar's date in the exchange zone. */
    public static final Spec<Bar> DAILY_BAR = bars("date", b -> b.time().toLocalDate(), Kind.DATE);

    /**
     * The curated fields of {@link TickerInfo}, as {@code info} prints them: identity, profile,
     * price, size and valuation, dividends, financials, and the fund-only fields.
     */
    public static final Spec<TickerInfo> TICKER_INFO = Spec.<TickerInfo>builder()
            .col("symbol", TickerInfo::symbol, Kind.TEXT)
            .col("quote_type", TickerInfo::quoteType, Kind.TEXT)
            .col("short_name", TickerInfo::shortName, Kind.TEXT)
            .col("long_name", TickerInfo::longName, Kind.TEXT)
            .col("currency", TickerInfo::currency, Kind.TEXT)
            .col("financial_currency", TickerInfo::financialCurrency, Kind.TEXT)
            .col("exchange", TickerInfo::exchange, Kind.TEXT)
            .col("full_exchange_name", TickerInfo::fullExchangeName, Kind.TEXT)
            .col("exchange_timezone", TickerInfo::exchangeTimezoneName, Kind.TEXT)
            .col("market", TickerInfo::market, Kind.TEXT)
            .col("market_state", TickerInfo::marketState, Kind.TEXT)
            .col("sector", TickerInfo::sector, Kind.TEXT)
            .col("industry", TickerInfo::industry, Kind.TEXT)
            .col("country", TickerInfo::country, Kind.TEXT)
            .col("website", TickerInfo::website, Kind.TEXT)
            .col("full_time_employees", TickerInfo::fullTimeEmployees, Kind.INTEGER)
            .col("current_price", TickerInfo::currentPrice, PRICE)
            .col("regular_market_price", TickerInfo::regularMarketPrice, PRICE)
            .col("previous_close", TickerInfo::previousClose, PRICE)
            .col("open", TickerInfo::open, PRICE)
            .col("day_high", TickerInfo::dayHigh, PRICE)
            .col("day_low", TickerInfo::dayLow, PRICE)
            .col("volume", TickerInfo::volume, Kind.INTEGER)
            .col("average_volume", TickerInfo::averageVolume, Kind.INTEGER)
            .col("fifty_two_week_high", TickerInfo::fiftyTwoWeekHigh, PRICE)
            .col("fifty_two_week_low", TickerInfo::fiftyTwoWeekLow, PRICE)
            .col("fifty_day_average", TickerInfo::fiftyDayAverage, PRICE)
            .col("two_hundred_day_average", TickerInfo::twoHundredDayAverage, PRICE)
            .col("beta", TickerInfo::beta, RATIO)
            .col("market_cap", TickerInfo::marketCap, Kind.INTEGER)
            .col("enterprise_value", TickerInfo::enterpriseValue, Kind.INTEGER)
            .col("shares_outstanding", TickerInfo::sharesOutstanding, Kind.INTEGER)
            .col("float_shares", TickerInfo::floatShares, Kind.INTEGER)
            .col("trailing_pe", TickerInfo::trailingPE, RATIO)
            .col("forward_pe", TickerInfo::forwardPE, RATIO)
            .col("trailing_peg_ratio", TickerInfo::trailingPegRatio, RATIO)
            .col("price_to_book", TickerInfo::priceToBook, RATIO)
            .col("price_to_sales", TickerInfo::priceToSalesTrailing12Months, RATIO)
            .col("trailing_eps", TickerInfo::trailingEps, PRICE)
            .col("forward_eps", TickerInfo::forwardEps, PRICE)
            .col("book_value", TickerInfo::bookValue, PRICE)
            .col("dividend_rate", TickerInfo::dividendRate, PRICE)
            // Yahoo reports dividendYield in percent already (0.32 = 0.32%), unlike the other ratios
            .col("dividend_yield", TickerInfo::dividendYield, RATIO)
            .col("payout_ratio", TickerInfo::payoutRatio, Kind.PERCENT)
            .col("ex_dividend_date", TickerInfo::exDividendDate, Kind.DATE)
            .col("total_revenue", TickerInfo::totalRevenue, Kind.INTEGER)
            .col("ebitda", TickerInfo::ebitda, Kind.INTEGER)
            .col("total_cash", TickerInfo::totalCash, Kind.INTEGER)
            .col("total_debt", TickerInfo::totalDebt, Kind.INTEGER)
            .col("profit_margins", TickerInfo::profitMargins, Kind.PERCENT)
            .col("return_on_equity", TickerInfo::returnOnEquity, Kind.PERCENT)
            .col("revenue_growth", TickerInfo::revenueGrowth, Kind.PERCENT)
            .col("earnings_growth", TickerInfo::earningsGrowth, Kind.PERCENT)
            .col("target_mean_price", TickerInfo::targetMeanPrice, PRICE)
            .col("recommendation", TickerInfo::recommendationKey, Kind.TEXT)
            .col("total_assets", TickerInfo::totalAssets, Kind.INTEGER)
            .col("yield", TickerInfo::yield, Kind.PERCENT)
            .col("category", TickerInfo::category, Kind.TEXT)
            .col("fund_family", TickerInfo::fundFamily, Kind.TEXT)
            .build();

    /** A symbol's ISIN, as {@code isin} prints it. */
    public static final Spec<Symbol<String>> ISIN = Spec.<Symbol<String>>builder()
            .col("symbol", Symbol::symbol, Kind.TEXT)
            .col("isin", Symbol::value, Kind.TEXT)
            .build();

    /**
     * The per-ticker {@link Calendar}, as {@code events} prints it. Yahoo gives the next earnings
     * date as a one- or two-day window: {@code earnings_date} is its first day and {@code
     * earnings_date_end} its last, when the window has two.
     */
    public static final Spec<Calendar> CALENDAR = Spec.<Calendar>builder()
            .col(
                    "earnings_date",
                    c -> c.earningsDates().isEmpty() ? null : c.earningsDates().getFirst(),
                    Kind.DATE)
            .col("earnings_date_end", Specs::earningsDateEnd, Kind.DATE)
            .col("earnings_date_estimate", Calendar::earningsDateEstimate, Kind.TEXT)
            .col("earnings_average", Calendar::earningsAverage, PRICE)
            .col("earnings_low", Calendar::earningsLow, PRICE)
            .col("earnings_high", Calendar::earningsHigh, PRICE)
            .col("revenue_average", Calendar::revenueAverage, Kind.INTEGER)
            .col("revenue_low", Calendar::revenueLow, Kind.INTEGER)
            .col("revenue_high", Calendar::revenueHigh, Kind.INTEGER)
            .col("ex_dividend_date", Calendar::exDividendDate, Kind.DATE)
            .col("dividend_date", Calendar::dividendDate, Kind.DATE)
            .build();

    /** ESG risk scores, as {@code sustainability} prints them. */
    public static final Spec<Sustainability> SUSTAINABILITY = Spec.<Sustainability>builder()
            .col("total_esg", Sustainability::totalEsg, RATIO)
            .col("environment_score", Sustainability::environmentScore, RATIO)
            .col("social_score", Sustainability::socialScore, RATIO)
            .col("governance_score", Sustainability::governanceScore, RATIO)
            .col("percentile", Sustainability::percentile, RATIO)
            .col("highest_controversy", Sustainability::highestControversy, RATIO)
            .col("esg_performance", Sustainability::esgPerformance, Kind.TEXT)
            .col("peer_group", Sustainability::peerGroup, Kind.TEXT)
            .col("peer_count", Sustainability::peerCount, Kind.INTEGER)
            .col("rating_year", Sustainability::ratingYear, Kind.INTEGER)
            .col("rating_month", Sustainability::ratingMonth, Kind.INTEGER)
            .col("related_controversy", s -> joined(s.relatedControversy()), Kind.TEXT)
            .build();

    /**
     * A fund's overview, as {@code fund} prints it: profile, the main operations metrics and the
     * asset-class split. Holdings and sector weights are separate lists ({@link #FUND_HOLDING},
     * {@link #FUND_SECTOR}).
     */
    public static final Spec<FundsData> FUND = Spec.<FundsData>builder()
            .col("symbol", FundsData::symbol, Kind.TEXT)
            .col("quote_type", FundsData::quoteType, Kind.TEXT)
            .col("category", FundsData::categoryName, Kind.TEXT)
            .col("family", FundsData::family, Kind.TEXT)
            .col("legal_type", FundsData::legalType, Kind.TEXT)
            .col("expense_ratio", f -> operation(f, "annualReportExpenseRatio"), Kind.PERCENT)
            .col("category_expense_ratio", f -> categoryOperation(f, "annualReportExpenseRatio"), Kind.PERCENT)
            .col("turnover", f -> operation(f, "annualHoldingsTurnover"), Kind.PERCENT)
            .col("total_net_assets", f -> operation(f, "totalNetAssets"), Kind.decimal(2))
            .col("cash_position", f -> f.assetClasses().get("cashPosition"), Kind.PERCENT)
            .col("stock_position", f -> f.assetClasses().get("stockPosition"), Kind.PERCENT)
            .col("bond_position", f -> f.assetClasses().get("bondPosition"), Kind.PERCENT)
            .col("preferred_position", f -> f.assetClasses().get("preferredPosition"), Kind.PERCENT)
            .col("convertible_position", f -> f.assetClasses().get("convertiblePosition"), Kind.PERCENT)
            .col("other_position", f -> f.assetClasses().get("otherPosition"), Kind.PERCENT)
            .col("description", FundsData::description, Kind.TEXT)
            .build();

    /** A fund's top holdings, as {@code fund --holdings} prints them. */
    public static final Spec<FundHolding> FUND_HOLDING = Spec.<FundHolding>builder()
            .col("symbol", FundHolding::symbol, Kind.TEXT)
            .col("name", FundHolding::name, Kind.TEXT)
            .col("weight", FundHolding::holdingPercent, Kind.PERCENT)
            .build();

    /** A fund's sector weights, as {@code fund --sectors} prints them: Yahoo's sector key and weight. */
    public static final Spec<Map.Entry<String, Double>> FUND_SECTOR = Spec.<Map.Entry<String, Double>>builder()
            .col("sector", Map.Entry::getKey, Kind.TEXT)
            .col("weight", Map.Entry::getValue, Kind.PERCENT)
            .build();

    /** Per-share cash amounts (dividends, capital gains): four fraction digits in tables. */
    public static final Kind AMOUNT = Kind.decimal(4);

    /** Dividends, as {@code dividends} prints them: the ex-date in the exchange zone and the amount. */
    public static final Spec<Dividend> DIVIDEND = Spec.<Dividend>builder()
            .col("date", Dividend::time, Kind.DATE)
            .col("amount", Dividend::amount, AMOUNT)
            .build();

    /** Stock splits, as {@code splits} prints them: the terms and their ratio ({@code 4} for 4:1). */
    public static final Spec<Split> SPLIT = Spec.<Split>builder()
            .col("date", Split::time, Kind.DATE)
            .col("numerator", Split::numerator, RATIO)
            .col("denominator", Split::denominator, RATIO)
            .col("ratio", Split::ratio, RATIO)
            .build();

    /** Capital gains distributions, as {@code capital-gains} prints them. */
    public static final Spec<CapitalGain> CAPITAL_GAIN = Spec.<CapitalGain>builder()
            .col("date", CapitalGain::time, Kind.DATE)
            .col("amount", CapitalGain::amount, AMOUNT)
            .build();

    /** Dividends, splits and capital gains merged by day, as {@code actions} prints them. */
    public static final Spec<ActionRow> ACTION = Spec.<ActionRow>builder()
            .col("date", ActionRow::date, Kind.DATE)
            .col("dividend", ActionRow::dividend, AMOUNT)
            .col("split", ActionRow::split, RATIO)
            .col("capital_gain", ActionRow::capitalGain, AMOUNT)
            .build();

    /** Shares outstanding, as {@code shares} prints them; Yahoo may report several per day. */
    public static final Spec<SharesCount> SHARES = Spec.<SharesCount>builder()
            .col("date", SharesCount::time, Kind.DATE)
            .col("shares", SharesCount::shares, Kind.INTEGER)
            .build();

    /** SEC filings, as {@code sec-filings} prints them; {@code exhibits} lists the exhibit types. */
    public static final Spec<SecFiling> SEC_FILING = Spec.<SecFiling>builder()
            .col("date", SecFiling::date, Kind.DATE)
            .col("type", SecFiling::type, Kind.TEXT)
            .col("title", SecFiling::title, Kind.TEXT)
            .col("edgar_url", SecFiling::edgarUrl, Kind.TEXT)
            .col("exhibits", f -> joined(List.copyOf(f.exhibits().keySet())), Kind.TEXT)
            .build();

    private Specs() {}

    /**
     * The bar spec for an interval: {@link #BAR} for intraday intervals, {@link #DAILY_BAR} otherwise.
     *
     * @param interval the bar size
     * @return the spec
     */
    public static Spec<Bar> bars(Interval interval) {
        return interval.isIntraday() ? BAR : DAILY_BAR;
    }

    private static Spec<Bar> bars(String timeName, Function<Bar, Object> time, Kind timeKind) {
        return Spec.<Bar>builder()
                .col(timeName, time, timeKind)
                .col("open", Bar::open, PRICE)
                .col("high", Bar::high, PRICE)
                .col("low", Bar::low, PRICE)
                .col("close", Bar::close, PRICE)
                .col("adj_close", Bar::adjClose, PRICE)
                .col("volume", Bar::volume, Kind.INTEGER)
                .build();
    }

    private static @Nullable Double change(FastInfo info) {
        Double last = info.lastPrice();
        Double previous = info.regularMarketPreviousClose();
        return last == null || previous == null ? null : last - previous;
    }

    private static @Nullable Double changePct(FastInfo info) {
        Double change = change(info);
        Double previous = info.regularMarketPreviousClose();
        return change == null || previous == null || previous == 0 ? null : change / previous;
    }

    private static @Nullable LocalDate earningsDateEnd(Calendar calendar) {
        List<LocalDate> dates = calendar.earningsDates();
        return dates.size() < 2 ? null : dates.getLast();
    }

    private static @Nullable Double operation(FundsData fund, String key) {
        FundsData.Metric metric = fund.operations().get(key);
        return metric == null ? null : metric.fund();
    }

    private static @Nullable Double categoryOperation(FundsData fund, String key) {
        FundsData.Metric metric = fund.operations().get(key);
        return metric == null ? null : metric.categoryAverage();
    }

    private static @Nullable String joined(List<String> values) {
        return values.isEmpty() ? null : String.join(", ", values);
    }
}
