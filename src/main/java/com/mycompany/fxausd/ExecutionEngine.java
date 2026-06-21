package com.mycompany.fxausd;

import com.mycompany.fxausd.Fxausd.LiveTrade;
import com.mycompany.fxausd.Fxausd.TradeRecord;
import com.mycompany.fxausd.Fxausd.TradeSignal;
import java.util.ArrayList;
import java.util.List;

public class ExecutionEngine {

    private final double accountBalanceUsd;
    private final AdaptiveRiskManager riskManager;
    private final double maxAsiaSpreadPips = 1.5;
    private final double maxLondonSpreadPips = 0.9;
    private final double allowedSlippagePips = parseDoubleEnv("ALLOWED_SLIPPAGE_PIPS", 0.7);
    private final List<LiveTrade> openTrades = new ArrayList<>();
    private final List<TradeRecord> tradeLog = new ArrayList<>();

    public ExecutionEngine(double accountBalanceUsd, AdaptiveRiskManager riskManager) {
        this.accountBalanceUsd = accountBalanceUsd;
        this.riskManager = riskManager;
    }

    public List<TradeRecord> getTradeLog() {
        return tradeLog;
    }

    public void executeSignals(List<TradeSignal> signals, List<Fxausd.Candle> candles, List<TradeRecord> history) {
        if (signals == null || signals.isEmpty()) {
            System.out.println("⚠️ No signals to execute.");
            return;
        }
        double riskPercent = riskManager.determineRiskPercent(history);
        if (riskManager.shouldPauseTrading(history)) {
            System.out.println("⚠️ Trading paused by adaptive risk manager due to drawdown.");
            return;
        }

        for (TradeSignal signal : signals) {
            if (!canTrade()) {
                System.out.println("⚠️ Execution halted: risk or open trade limits reached.");
                break;
            }
            if (signal == null) {
                continue;
            }
            signal.session = signal.session == null ? PrecisionFilter.determineSession(signal.timestamp) : signal.session;
            double currentSpread = getCurrentSpreadPips();
            if (!isSpreadAcceptable(signal.session, currentSpread)) {
                System.out.println("⚠️ Skipping " + signal.symbol + " because spread " + currentSpread + " is too wide for " + signal.session + ".");
                continue;
            }
            int entryIndex = Math.min(Math.max(0, signal.sourceIndex + 1), candles.size() - 1);
            Fxausd.Candle entryCandle = candles.get(entryIndex);
            if (isLateCandle(entryCandle, candles, entryIndex)) {
                System.out.println("⚠️ Skipping late candle entry for " + signal.symbol + ".");
                continue;
            }
            if (!hasRetestEntry(signal, candles, entryIndex)) {
                System.out.println("⚠️ Skipping " + signal.symbol + " until breakout zone retest occurs.");
                continue;
            }
            if (isHighImpactNewsWindow()) {
                System.out.println("⚠️ Skipping " + signal.symbol + " because a high-impact news window is active.");
                continue;
            }
            double riskUsd = accountBalanceUsd * riskPercent;
            double lotSize = Math.max(0.01, Math.min(1.0, riskUsd / (signal.riskAmount * 10.0)));
            if (lotSize <= 0) {
                System.out.println("⚠️ Position sizing failed for " + signal.symbol + ".");
                continue;
            }
            double expectedPrice = entryCandle.open;
            double fillPrice = expectedPrice + (signal.direction.equals("BUY") ? currentSpread * 0.0001 : -currentSpread * 0.0001);
            if (Math.abs(fillPrice - expectedPrice) > allowedSlippagePips * 0.0001) {
                System.out.println("⚠️ Canceling " + signal.symbol + " due to slippage guard.");
                continue;
            }
            double adjustedStopLoss = signal.stopLoss + (fillPrice - signal.entry);
            double adjustedTakeProfit = signal.takeProfit + (fillPrice - signal.entry);
            boolean sent = Fxausd.sendSignalToMT5(signal);
            if (!sent) {
                System.out.println("❌ Failed live trade dispatch: " + signal.symbol + " " + signal.direction);
                continue;
            }
            LiveTrade trade = new LiveTrade(signal, fillPrice, adjustedStopLoss, adjustedTakeProfit,
                    lotSize, signal.riskAmount, signal.rewardAmount, entryIndex);
            openTrades.add(trade);
            System.out.println("📈 Executed live trade: " + signal.symbol + " " + signal.direction + " @ " + String.format("%.5f", fillPrice));
            processFutureBars(candles, entryIndex + 1);
            if (!trade.isOpen()) {
                TradeRecord record = new TradeRecord(signal, trade);
                tradeLog.add(record);
                Fxausd.recordTrade(record);
            }
        }
        for (LiveTrade open : new ArrayList<>(openTrades)) {
            if (!open.isOpen()) {
                TradeRecord record = new TradeRecord(open.signal, open);
                tradeLog.add(record);
                Fxausd.recordTrade(record);
            }
        }
        Fxausd.exportTradeLogToCSV(tradeLog, "trade_history.csv");
    }

    private boolean canTrade() {
        return openTrades.size() < 5;
    }

    private boolean isSpreadAcceptable(String session, double spreadPips) {
        if (session == null) {
            return false;
        }
        if (session.equalsIgnoreCase("ASIA")) {
            return spreadPips <= maxAsiaSpreadPips;
        }
        if (session.equalsIgnoreCase("LONDON")) {
            return spreadPips <= maxLondonSpreadPips;
        }
        return spreadPips <= 1.2;
    }

    private boolean isLateCandle(Fxausd.Candle candle, List<Fxausd.Candle> candles, int index) {
        if (candle == null || candles == null || index < 0 || index >= candles.size()) {
            return false;
        }
        double atr = Fxausd.calculateATR(candles, index, 14);
        double range = Math.abs(candle.high - candle.low);
        return range > atr * 1.8;
    }

    private boolean hasRetestEntry(TradeSignal signal, List<Fxausd.Candle> candles, int entryIndex) {
        if (signal == null || candles == null || candles.isEmpty()) {
            return false;
        }
        double zoneLow = Math.min(signal.entry, signal.stopLoss);
        double zoneHigh = Math.max(signal.entry, signal.stopLoss);
        int lookback = Math.min(entryIndex, 5);
        for (int i = entryIndex - lookback; i < entryIndex; i++) {
            if (i < 0 || i >= candles.size()) {
                continue;
            }
            Fxausd.Candle prior = candles.get(i);
            if (signal.direction.equals("BUY") && prior.low <= zoneHigh && prior.high >= zoneLow) {
                return true;
            }
            if (signal.direction.equals("SELL") && prior.high >= zoneLow && prior.low <= zoneHigh) {
                return true;
            }
        }
        return false;
    }

    private boolean isHighImpactNewsWindow() {
        String env = System.getenv("HIGH_IMPACT_NEWS");
        return env != null && (env.equalsIgnoreCase("1") || env.equalsIgnoreCase("true"));
    }

    private double getCurrentSpreadPips() {
        String env = System.getenv("CURRENT_SPREAD_PIPS");
        if (env == null || env.trim().isEmpty()) {
            return 1.0;
        }
        try {
            return Double.parseDouble(env.trim());
        } catch (NumberFormatException ignored) {
            return 1.0;
        }
    }

    private static double parseDoubleEnv(String envName, double fallback) {
        String env = System.getenv(envName);
        if (env == null || env.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Double.parseDouble(env.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void processFutureBars(List<Fxausd.Candle> candles, int startIndex) {
        for (int i = startIndex; i < candles.size(); i++) {
            Fxausd.Candle candle = candles.get(i);
            for (LiveTrade trade : new ArrayList<>(openTrades)) {
                if (!trade.isOpen()) {
                    continue;
                }
                trade.update(candle);
                if (!trade.isOpen()) {
                    openTrades.remove(trade);
                }
            }
            if (openTrades.isEmpty()) {
                break;
            }
        }
    }
}
