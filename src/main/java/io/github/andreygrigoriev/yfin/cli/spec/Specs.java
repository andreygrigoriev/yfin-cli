package io.github.andreygrigoriev.yfin.cli.spec;

import io.github.andreygrigoriev.yfin.FastInfo;
import io.github.andreygrigoriev.yfin.cli.output.Kind;
import io.github.andreygrigoriev.yfin.cli.output.Spec;
import io.github.andreygrigoriev.yfin.cli.output.Symbol;
import io.github.andreygrigoriev.yfin.model.Bar;
import io.github.andreygrigoriev.yfin.model.Calendar;
import io.github.andreygrigoriev.yfin.model.CapitalGain;
import io.github.andreygrigoriev.yfin.model.Dividend;
import io.github.andreygrigoriev.yfin.model.EarningsDate;
import io.github.andreygrigoriev.yfin.model.EarningsEstimate;
import io.github.andreygrigoriev.yfin.model.EarningsEvent;
import io.github.andreygrigoriev.yfin.model.EarningsHistoryEntry;
import io.github.andreygrigoriev.yfin.model.EpsRevisions;
import io.github.andreygrigoriev.yfin.model.EpsTrend;
import io.github.andreygrigoriev.yfin.model.FundHolding;
import io.github.andreygrigoriev.yfin.model.FundsData;
import io.github.andreygrigoriev.yfin.model.GrowthCompany;
import io.github.andreygrigoriev.yfin.model.GrowthEstimate;
import io.github.andreygrigoriev.yfin.model.Holder;
import io.github.andreygrigoriev.yfin.model.IndustryData;
import io.github.andreygrigoriev.yfin.model.InsiderRosterEntry;
import io.github.andreygrigoriev.yfin.model.InsiderTransaction;
import io.github.andreygrigoriev.yfin.model.IpoEvent;
import io.github.andreygrigoriev.yfin.model.LookupResult;
import io.github.andreygrigoriev.yfin.model.MajorHolders;
import io.github.andreygrigoriev.yfin.model.MarketStatus;
import io.github.andreygrigoriev.yfin.model.MarketSummaryItem;
import io.github.andreygrigoriev.yfin.model.NewsItem;
import io.github.andreygrigoriev.yfin.model.PerformingCompany;
import io.github.andreygrigoriev.yfin.model.PriceTargets;
import io.github.andreygrigoriev.yfin.model.Recommendation;
import io.github.andreygrigoriev.yfin.model.RevenueEstimate;
import io.github.andreygrigoriev.yfin.model.SearchQuote;
import io.github.andreygrigoriev.yfin.model.SecFiling;
import io.github.andreygrigoriev.yfin.model.SectorData;
import io.github.andreygrigoriev.yfin.model.SectorIndustry;
import io.github.andreygrigoriev.yfin.model.SharesCount;
import io.github.andreygrigoriev.yfin.model.Split;
import io.github.andreygrigoriev.yfin.model.SplitEvent;
import io.github.andreygrigoriev.yfin.model.Sustainability;
import io.github.andreygrigoriev.yfin.model.TickerInfo;
import io.github.andreygrigoriev.yfin.model.TopCompany;
import io.github.andreygrigoriev.yfin.model.UpgradeDowngrade;
import io.github.andreygrigoriev.yfin.request.Interval;
import java.math.BigDecimal;
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

    /** The ownership breakdown, as {@code holders major} prints it; fractions of shares or float. */
    public static final Spec<MajorHolders> MAJOR_HOLDERS = Spec.<MajorHolders>builder()
            .col("insiders_pct_held", MajorHolders::insidersPercentHeld, Kind.PERCENT)
            .col("institutions_pct_held", MajorHolders::institutionsPercentHeld, Kind.PERCENT)
            .col("institutions_float_pct_held", MajorHolders::institutionsFloatPercentHeld, Kind.PERCENT)
            .col("institutions_count", MajorHolders::institutionsCount, Kind.INTEGER)
            .build();

    /**
     * Institutional and mutual fund holders, as {@code holders institutional|funds} print them.
     * {@code pct_change} is the change of the position since the previous filing.
     */
    public static final Spec<Holder> HOLDER = Spec.<Holder>builder()
            .col("date_reported", Holder::dateReported, Kind.DATE)
            .col("holder", Holder::holder, Kind.TEXT)
            .col("shares", Holder::shares, Kind.INTEGER)
            .col("value", Holder::value, Kind.INTEGER)
            .col("pct_held", Holder::pctHeld, Kind.PERCENT)
            .col("pct_change", Holder::pctChange, Kind.PERCENT)
            .build();

    /** Insiders and their positions, as {@code holders insiders} prints them. */
    public static final Spec<InsiderRosterEntry> INSIDER = Spec.<InsiderRosterEntry>builder()
            .col("name", InsiderRosterEntry::name, Kind.TEXT)
            .col("position", InsiderRosterEntry::position, Kind.TEXT)
            .col("most_recent_transaction", InsiderRosterEntry::mostRecentTransaction, Kind.TEXT)
            .col("latest_transaction_date", InsiderRosterEntry::latestTransactionDate, Kind.DATE)
            .col("shares_owned_directly", InsiderRosterEntry::sharesOwnedDirectly, Kind.INTEGER)
            .col("position_direct_date", InsiderRosterEntry::positionDirectDate, Kind.DATE)
            .col("shares_owned_indirectly", InsiderRosterEntry::sharesOwnedIndirectly, Kind.INTEGER)
            .col("position_indirect_date", InsiderRosterEntry::positionIndirectDate, Kind.DATE)
            .col("url", InsiderRosterEntry::url, Kind.TEXT)
            .build();

    /**
     * Insider transactions, as {@code holders insider-tx} prints them. {@code ownership} is {@code
     * D} (direct) or {@code I} (indirect); {@code transaction} is Yahoo's often empty money text.
     */
    public static final Spec<InsiderTransaction> INSIDER_TRANSACTION = Spec.<InsiderTransaction>builder()
            .col("date", InsiderTransaction::startDate, Kind.DATE)
            .col("insider", InsiderTransaction::insider, Kind.TEXT)
            .col("position", InsiderTransaction::position, Kind.TEXT)
            .col("text", InsiderTransaction::text, Kind.TEXT)
            .col("shares", InsiderTransaction::shares, Kind.INTEGER)
            .col("value", InsiderTransaction::value, Kind.INTEGER)
            .col("ownership", InsiderTransaction::ownership, Kind.TEXT)
            .col("transaction", InsiderTransaction::transaction, Kind.TEXT)
            .col("url", InsiderTransaction::url, Kind.TEXT)
            .build();

    /**
     * EPS estimates per period, as {@code estimates earnings} prints them. {@code period} is {@code
     * 0q}/{@code +1q} (current/next quarter) or {@code 0y}/{@code +1y} (fiscal year); {@code growth}
     * is over {@code year_ago_eps}.
     */
    public static final Spec<EarningsEstimate> EARNINGS_ESTIMATE = Spec.<EarningsEstimate>builder()
            .col("period", EarningsEstimate::period, Kind.TEXT)
            .col("end_date", EarningsEstimate::endDate, Kind.DATE)
            .col("analysts", EarningsEstimate::numberOfAnalysts, Kind.INTEGER)
            .col("avg", EarningsEstimate::avg, PRICE)
            .col("low", EarningsEstimate::low, PRICE)
            .col("high", EarningsEstimate::high, PRICE)
            .col("year_ago_eps", EarningsEstimate::yearAgoEps, PRICE)
            .col("growth", EarningsEstimate::growth, Kind.PERCENT)
            .col("currency", EarningsEstimate::currency, Kind.TEXT)
            .build();

    /** Revenue estimates per period, as {@code estimates revenue} prints them. */
    public static final Spec<RevenueEstimate> REVENUE_ESTIMATE = Spec.<RevenueEstimate>builder()
            .col("period", RevenueEstimate::period, Kind.TEXT)
            .col("end_date", RevenueEstimate::endDate, Kind.DATE)
            .col("analysts", RevenueEstimate::numberOfAnalysts, Kind.INTEGER)
            .col("avg", RevenueEstimate::avg, Kind.INTEGER)
            .col("low", RevenueEstimate::low, Kind.INTEGER)
            .col("high", RevenueEstimate::high, Kind.INTEGER)
            .col("year_ago_revenue", RevenueEstimate::yearAgoRevenue, Kind.INTEGER)
            .col("growth", RevenueEstimate::growth, Kind.PERCENT)
            .col("currency", RevenueEstimate::currency, Kind.TEXT)
            .build();

    /** Reported versus estimated EPS of past quarters, as {@code estimates history} prints them. */
    public static final Spec<EarningsHistoryEntry> EARNINGS_HISTORY = Spec.<EarningsHistoryEntry>builder()
            .col("quarter", EarningsHistoryEntry::quarter, Kind.DATE)
            .col("period", EarningsHistoryEntry::period, Kind.TEXT)
            .col("eps_estimate", EarningsHistoryEntry::epsEstimate, PRICE)
            .col("eps_actual", EarningsHistoryEntry::epsActual, PRICE)
            .col("eps_difference", EarningsHistoryEntry::epsDifference, PRICE)
            .col("surprise", EarningsHistoryEntry::surprisePercent, Kind.PERCENT)
            .col("currency", EarningsHistoryEntry::currency, Kind.TEXT)
            .build();

    /** How the mean EPS estimate moved, as {@code estimates eps-trend} prints it. */
    public static final Spec<EpsTrend> EPS_TREND = Spec.<EpsTrend>builder()
            .col("period", EpsTrend::period, Kind.TEXT)
            .col("end_date", EpsTrend::endDate, Kind.DATE)
            .col("current", EpsTrend::current, PRICE)
            .col("7_days_ago", EpsTrend::sevenDaysAgo, PRICE)
            .col("30_days_ago", EpsTrend::thirtyDaysAgo, PRICE)
            .col("60_days_ago", EpsTrend::sixtyDaysAgo, PRICE)
            .col("90_days_ago", EpsTrend::ninetyDaysAgo, PRICE)
            .col("currency", EpsTrend::currency, Kind.TEXT)
            .build();

    /** Analysts revising their EPS estimate, as {@code estimates eps-revisions} prints them. */
    public static final Spec<EpsRevisions> EPS_REVISIONS = Spec.<EpsRevisions>builder()
            .col("period", EpsRevisions::period, Kind.TEXT)
            .col("end_date", EpsRevisions::endDate, Kind.DATE)
            .col("up_last_7_days", EpsRevisions::upLast7Days, Kind.INTEGER)
            .col("up_last_30_days", EpsRevisions::upLast30Days, Kind.INTEGER)
            .col("down_last_7_days", EpsRevisions::downLast7Days, Kind.INTEGER)
            .col("down_last_30_days", EpsRevisions::downLast30Days, Kind.INTEGER)
            .col("currency", EpsRevisions::currency, Kind.TEXT)
            .build();

    /** Expected growth of the symbol and its benchmarks, as {@code estimates growth} prints it. */
    public static final Spec<GrowthEstimate> GROWTH_ESTIMATE = Spec.<GrowthEstimate>builder()
            .col("period", GrowthEstimate::period, Kind.TEXT)
            .col("stock", GrowthEstimate::stock, Kind.PERCENT)
            .col("industry", GrowthEstimate::industry, Kind.PERCENT)
            .col("sector", GrowthEstimate::sector, Kind.PERCENT)
            .col("index", GrowthEstimate::index, Kind.PERCENT)
            .build();

    /** Rating counts per month ({@code 0m} is the current month), as {@code recommendations} prints them. */
    public static final Spec<Recommendation> RECOMMENDATION = Spec.<Recommendation>builder()
            .col("period", Recommendation::period, Kind.TEXT)
            .col("strong_buy", Recommendation::strongBuy, Kind.INTEGER)
            .col("buy", Recommendation::buy, Kind.INTEGER)
            .col("hold", Recommendation::hold, Kind.INTEGER)
            .col("sell", Recommendation::sell, Kind.INTEGER)
            .col("strong_sell", Recommendation::strongSell, Kind.INTEGER)
            .build();

    /**
     * Analyst rating changes, as {@code upgrades} prints them. Yahoo gives the publication time as
     * an instant, printed in UTC; a price target of {@code 0} means the firm gave none.
     */
    public static final Spec<UpgradeDowngrade> UPGRADE_DOWNGRADE = Spec.<UpgradeDowngrade>builder()
            .col("time", UpgradeDowngrade::gradeDate, Kind.TIMESTAMP)
            .col("firm", UpgradeDowngrade::firm, Kind.TEXT)
            .col("to_grade", UpgradeDowngrade::toGrade, Kind.TEXT)
            .col("from_grade", UpgradeDowngrade::fromGrade, Kind.TEXT)
            .col("action", UpgradeDowngrade::action, Kind.TEXT)
            .col("price_target_action", UpgradeDowngrade::priceTargetAction, Kind.TEXT)
            .col("current_price_target", UpgradeDowngrade::currentPriceTarget, PRICE)
            .col("prior_price_target", UpgradeDowngrade::priorPriceTarget, PRICE)
            .build();

    /** Analyst price targets next to the current price, as {@code price-target} prints them. */
    public static final Spec<PriceTargets> PRICE_TARGETS = Spec.<PriceTargets>builder()
            .col("current", PriceTargets::current, PRICE)
            .col("low", PriceTargets::low, PRICE)
            .col("mean", PriceTargets::mean, PRICE)
            .col("median", PriceTargets::median, PRICE)
            .col("high", PriceTargets::high, PRICE)
            .build();

    /**
     * Past and upcoming earnings announcements, as {@code earnings} prints them: the time in the
     * exchange zone. {@code surprise_pct} is in percent as Yahoo reports it ({@code 6.74} = 6.74%
     * beat), so it is a plain number rather than a fraction.
     */
    public static final Spec<EarningsDate> EARNINGS_DATE = Spec.<EarningsDate>builder()
            .col("time", EarningsDate::date, Kind.TIMESTAMP)
            .col("eps_estimate", EarningsDate::epsEstimate, PRICE)
            .col("reported_eps", EarningsDate::reportedEps, PRICE)
            .col("surprise_pct", EarningsDate::surprisePercent, RATIO)
            .build();

    /** Option expiration dates, as {@code options} without {@code --expiry} prints them. */
    public static final Spec<LocalDate> OPTION_EXPIRATION =
            Spec.<LocalDate>builder().col("expiry", d -> d, Kind.DATE).build();

    /**
     * Option contracts, as {@code options --expiry} prints them. {@code change_pct} is a fraction
     * like the other percentages (Yahoo gives percent); {@code implied_volatility} is a fraction.
     */
    public static final Spec<OptionRow> OPTION_CONTRACT = Spec.<OptionRow>builder()
            .col("contract_symbol", r -> r.contract().contractSymbol(), Kind.TEXT)
            .col("type", OptionRow::type, Kind.TEXT)
            .col("strike", r -> r.contract().strike(), PRICE)
            .col("last", r -> r.contract().lastPrice(), PRICE)
            .col("bid", r -> r.contract().bid(), PRICE)
            .col("ask", r -> r.contract().ask(), PRICE)
            .col("change", r -> r.contract().change(), PRICE)
            .col("change_pct", r -> fraction(r.contract().percentChange()), Kind.PERCENT)
            .col("volume", r -> r.contract().volume(), Kind.INTEGER)
            .col("open_interest", r -> r.contract().openInterest(), Kind.INTEGER)
            .col("implied_volatility", r -> r.contract().impliedVolatility(), Kind.PERCENT)
            .col("in_the_money", r -> r.contract().inTheMoney(), Kind.BOOLEAN)
            .col("last_trade", r -> r.contract().lastTradeDate(), Kind.TIMESTAMP)
            .col("currency", r -> r.contract().currency(), Kind.TEXT)
            .col("expiry", OptionRow::expiry, Kind.DATE)
            .build();

    /** Search scores and lookup ranks: whole numbers, never humanised in tables below a million. */
    public static final Kind SCORE = Kind.decimal(0);

    /**
     * News articles, as {@code news} and {@code search --news} print them: the publication time in
     * UTC, the related tickers comma-separated.
     */
    public static final Spec<NewsItem> NEWS = Spec.<NewsItem>builder()
            .col("time", NewsItem::publishTime, Kind.TIMESTAMP)
            .col("title", NewsItem::title, Kind.TEXT)
            .col("publisher", NewsItem::publisher, Kind.TEXT)
            .col("type", NewsItem::contentType, Kind.TEXT)
            .col("related_tickers", n -> joined(n.relatedTickers()), Kind.TEXT)
            .col("link", NewsItem::link, Kind.TEXT)
            .col("summary", NewsItem::summary, Kind.TEXT)
            .col("id", NewsItem::id, Kind.TEXT)
            .build();

    /** Search matches, as {@code search} prints them, best match first. */
    public static final Spec<SearchQuote> SEARCH_QUOTE = Spec.<SearchQuote>builder()
            .col("symbol", SearchQuote::symbol, Kind.TEXT)
            .col("short_name", SearchQuote::shortName, Kind.TEXT)
            .col("long_name", SearchQuote::longName, Kind.TEXT)
            .col("type", SearchQuote::typeDisplay, Kind.TEXT)
            .col("quote_type", SearchQuote::quoteType, Kind.TEXT)
            .col("exchange", SearchQuote::exchange, Kind.TEXT)
            .col("exchange_name", SearchQuote::exchangeDisplay, Kind.TEXT)
            .col("sector", SearchQuote::sector, Kind.TEXT)
            .col("industry", SearchQuote::industry, Kind.TEXT)
            .col("score", SearchQuote::score, SCORE)
            .build();

    /**
     * Lookup matches, as {@code lookup} prints them, in Yahoo's order. {@code change_pct} is a
     * fraction like the other percentages (Yahoo gives percent).
     */
    public static final Spec<LookupResult> LOOKUP_RESULT = Spec.<LookupResult>builder()
            .col("symbol", LookupResult::symbol, Kind.TEXT)
            .col("short_name", LookupResult::shortName, Kind.TEXT)
            .col("quote_type", LookupResult::quoteType, Kind.TEXT)
            .col("exchange", LookupResult::exchange, Kind.TEXT)
            .col("price", LookupResult::regularMarketPrice, PRICE)
            .col("change", LookupResult::regularMarketChange, PRICE)
            .col("change_pct", r -> fraction(r.regularMarketPercentChange()), Kind.PERCENT)
            .col("rank", LookupResult::rank, SCORE)
            .build();

    /** Names of Yahoo's predefined screens, as {@code screen --list} prints them. */
    public static final Spec<String> SCREEN_NAME =
            Spec.<String>builder().col("name", n -> n, Kind.TEXT).build();

    /**
     * Screener matches, as {@code screen} prints them, in the screen's order. Yahoo gives the
     * fields as a loose map; {@code change_pct} is a fraction like the other percentages (Yahoo
     * gives percent).
     */
    public static final Spec<Map<String, Object>> SCREEN_QUOTE = Spec.<Map<String, Object>>builder()
            .col("symbol", q -> q.get("symbol"), Kind.TEXT)
            .col("short_name", q -> q.get("shortName"), Kind.TEXT)
            .col("price", q -> number(q, "regularMarketPrice"), PRICE)
            .col("change", q -> number(q, "regularMarketChange"), PRICE)
            .col("change_pct", q -> fraction(number(q, "regularMarketChangePercent")), Kind.PERCENT)
            .col("volume", q -> number(q, "regularMarketVolume"), Kind.INTEGER)
            .col("avg_volume_3m", q -> number(q, "averageDailyVolume3Month"), Kind.INTEGER)
            .col("market_cap", q -> number(q, "marketCap"), Kind.INTEGER)
            .col("trailing_pe", q -> number(q, "trailingPE"), RATIO)
            .col("currency", q -> q.get("currency"), Kind.TEXT)
            .col("exchange", q -> q.get("exchange"), Kind.TEXT)
            .build();

    /** A sector's overview, as {@code sector} prints it. */
    public static final Spec<SectorData> SECTOR = Spec.<SectorData>builder()
            .col("key", SectorData::key, Kind.TEXT)
            .col("name", SectorData::name, Kind.TEXT)
            .col("symbol", SectorData::symbol, Kind.TEXT)
            .col("companies_count", s -> s.overview().companiesCount(), Kind.INTEGER)
            .col("industries_count", s -> s.overview().industriesCount(), Kind.INTEGER)
            .col("market_cap", s -> s.overview().marketCap(), Kind.INTEGER)
            .col("market_weight", s -> s.overview().marketWeight(), Kind.PERCENT)
            .col("employee_count", s -> s.overview().employeeCount(), Kind.INTEGER)
            .col("description", s -> s.overview().description(), Kind.TEXT)
            .build();

    /** A sector's industries, as {@code sector --industries} prints them, with their weight in the sector. */
    public static final Spec<SectorIndustry> SECTOR_INDUSTRY = Spec.<SectorIndustry>builder()
            .col("key", SectorIndustry::key, Kind.TEXT)
            .col("name", SectorIndustry::name, Kind.TEXT)
            .col("symbol", SectorIndustry::symbol, Kind.TEXT)
            .col("market_weight", SectorIndustry::marketWeight, Kind.PERCENT)
            .build();

    /** The largest companies of a sector or industry, as {@code sector --top} and {@code industry --top} print them. */
    public static final Spec<TopCompany> TOP_COMPANY = Spec.<TopCompany>builder()
            .col("symbol", TopCompany::symbol, Kind.TEXT)
            .col("name", TopCompany::name, Kind.TEXT)
            .col("rating", TopCompany::rating, Kind.TEXT)
            .col("market_weight", TopCompany::marketWeight, Kind.PERCENT)
            .build();

    /** An industry's overview, as {@code industry} prints it. */
    public static final Spec<IndustryData> INDUSTRY = Spec.<IndustryData>builder()
            .col("key", IndustryData::key, Kind.TEXT)
            .col("name", IndustryData::name, Kind.TEXT)
            .col("symbol", IndustryData::symbol, Kind.TEXT)
            .col("sector_key", IndustryData::sectorKey, Kind.TEXT)
            .col("sector_name", IndustryData::sectorName, Kind.TEXT)
            .col("companies_count", i -> i.overview().companiesCount(), Kind.INTEGER)
            .col("market_cap", i -> i.overview().marketCap(), Kind.INTEGER)
            .col("market_weight", i -> i.overview().marketWeight(), Kind.PERCENT)
            .col("employee_count", i -> i.overview().employeeCount(), Kind.INTEGER)
            .col("description", i -> i.overview().description(), Kind.TEXT)
            .build();

    /** An industry's best performers this year, as {@code industry --performing} prints them. */
    public static final Spec<PerformingCompany> PERFORMING_COMPANY = Spec.<PerformingCompany>builder()
            .col("symbol", PerformingCompany::symbol, Kind.TEXT)
            .col("name", PerformingCompany::name, Kind.TEXT)
            .col("ytd_return", PerformingCompany::ytdReturn, Kind.PERCENT)
            .col("last_price", PerformingCompany::lastPrice, PRICE)
            .col("target_price", PerformingCompany::targetPrice, PRICE)
            .build();

    /** An industry's fastest expected growers, as {@code industry --growth} prints them. */
    public static final Spec<GrowthCompany> GROWTH_COMPANY = Spec.<GrowthCompany>builder()
            .col("symbol", GrowthCompany::symbol, Kind.TEXT)
            .col("name", GrowthCompany::name, Kind.TEXT)
            .col("ytd_return", GrowthCompany::ytdReturn, Kind.PERCENT)
            .col("growth_estimate", GrowthCompany::growthEstimate, Kind.PERCENT)
            .build();

    /**
     * A region's headline instruments, as {@code market} prints them. {@code change_pct} is a
     * fraction like the other percentages (Yahoo gives percent); the time is printed in UTC.
     */
    public static final Spec<MarketSummaryItem> MARKET_SUMMARY = Spec.<MarketSummaryItem>builder()
            .col("symbol", MarketSummaryItem::symbol, Kind.TEXT)
            .col("short_name", MarketSummaryItem::shortName, Kind.TEXT)
            .col("quote_type", MarketSummaryItem::quoteType, Kind.TEXT)
            .col("exchange", MarketSummaryItem::exchange, Kind.TEXT)
            .col("price", MarketSummaryItem::price, PRICE)
            .col("change", MarketSummaryItem::change, PRICE)
            .col("change_pct", m -> fraction(m.changePercent()), Kind.PERCENT)
            .col("previous_close", MarketSummaryItem::previousClose, PRICE)
            .col("time", MarketSummaryItem::time, Kind.TIMESTAMP)
            .col("market_state", MarketSummaryItem::marketState, Kind.TEXT)
            .build();

    /** Today's trading hours of a market, as {@code market --status} prints them, in the market's zone. */
    public static final Spec<MarketStatus> MARKET_STATUS = Spec.<MarketStatus>builder()
            .col("id", MarketStatus::id, Kind.TEXT)
            .col("name", MarketStatus::name, Kind.TEXT)
            .col("status", MarketStatus::status, Kind.TEXT)
            .col("message", MarketStatus::message, Kind.TEXT)
            .col("open", MarketStatus::open, Kind.TIMESTAMP)
            .col("close", MarketStatus::close, Kind.TIMESTAMP)
            .col("zone", s -> s.zone().getId(), Kind.TEXT)
            .col("zone_short_name", MarketStatus::zoneShortName, Kind.TEXT)
            .build();

    /**
     * Market-wide earnings announcements, as {@code calendar earnings} prints them, in {@code
     * America/New_York}. {@code timing} is Yahoo's code ({@code BMO}, {@code AMC}, {@code TAS},
     * {@code TNS}); {@code surprise_pct} is in percent as Yahoo gives it, as in {@link #EARNINGS_DATE}.
     */
    public static final Spec<EarningsEvent> EARNINGS_EVENT = Spec.<EarningsEvent>builder()
            .col("symbol", EarningsEvent::symbol, Kind.TEXT)
            .col("company", EarningsEvent::companyName, Kind.TEXT)
            .col("market_cap", EarningsEvent::marketCap, Kind.INTEGER)
            .col("event", EarningsEvent::eventName, Kind.TEXT)
            .col("time", EarningsEvent::startTime, Kind.TIMESTAMP)
            .col("timing", EarningsEvent::timing, Kind.TEXT)
            .col("eps_estimate", EarningsEvent::epsEstimate, PRICE)
            .col("reported_eps", EarningsEvent::reportedEps, PRICE)
            .col("surprise_pct", EarningsEvent::surprisePercent, RATIO)
            .build();

    /** IPOs, as {@code calendar ipo} prints them; dates are in {@code America/New_York}. */
    public static final Spec<IpoEvent> IPO_EVENT = Spec.<IpoEvent>builder()
            .col("symbol", IpoEvent::symbol, Kind.TEXT)
            .col("company", IpoEvent::companyName, Kind.TEXT)
            .col("exchange", IpoEvent::exchange, Kind.TEXT)
            .col("date", IpoEvent::date, Kind.DATE)
            .col("filing_date", IpoEvent::filingDate, Kind.DATE)
            .col("amended_date", IpoEvent::amendedDate, Kind.DATE)
            .col("price_from", IpoEvent::priceFrom, PRICE)
            .col("price_to", IpoEvent::priceTo, PRICE)
            .col("offer_price", IpoEvent::offerPrice, PRICE)
            .col("currency", IpoEvent::currency, Kind.TEXT)
            .col("shares", IpoEvent::shares, Kind.INTEGER)
            .col("deal_type", IpoEvent::dealType, Kind.TEXT)
            .build();

    /**
     * Market-wide splits, as {@code calendar splits} prints them: {@code old_shares} become {@code
     * new_shares}, and {@code ratio} is their quotient as in {@link #SPLIT}.
     */
    public static final Spec<SplitEvent> SPLIT_EVENT = Spec.<SplitEvent>builder()
            .col("symbol", SplitEvent::symbol, Kind.TEXT)
            .col("company", SplitEvent::companyName, Kind.TEXT)
            .col("date", SplitEvent::payableDate, Kind.DATE)
            .col("optionable", SplitEvent::optionable, Kind.BOOLEAN)
            .col("old_shares", SplitEvent::oldShareWorth, RATIO)
            .col("new_shares", SplitEvent::newShareWorth, RATIO)
            .col("ratio", SplitEvent::ratio, RATIO)
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

    /** {@code 2.97} (percent) as the fraction {@code 0.0297}, shifted exactly; {@code null} for NaN. */
    private static @Nullable BigDecimal fraction(double percent) {
        return Double.isNaN(percent) ? null : BigDecimal.valueOf(percent).movePointLeft(2);
    }

    /** {@link #fraction(double)} of a value Yahoo may not have; {@code null} when missing. */
    private static @Nullable BigDecimal fraction(@Nullable Double percent) {
        return percent == null ? null : fraction(percent.doubleValue());
    }

    /** A numeric field of a loose Yahoo map; {@code null} when missing or not a number. */
    private static @Nullable Double number(Map<String, Object> map, String key) {
        return map.get(key) instanceof Number n ? n.doubleValue() : null;
    }

    private static @Nullable String joined(List<String> values) {
        return values.isEmpty() ? null : String.join(", ", values);
    }
}
