package com.mycompany.fxausd;

import com.mycompany.fxausd.Fxausd.TradeRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public class AdaptiveRiskManager {

    public static final double WINNING_MODE_PERCENT = 0.015;
    public static final double NEUTRAL_MODE_PERCENT = 0.010;
    public static final double LOSING_MODE_PERCENT = 0.004;
    public static final double DRAWDOWN_PAUSE_THRESHOLD = 0.05;

    public double determineRiskPercent(List<TradeRecord> history) {
        double baseRisk = NEUTRAL_MODE_PERCENT;
        
        // World Class Adjustment: Market Heartbeat Awareness
        if (Fxausd.currentIntel.institutionalDisplacement > 1.2) {
            baseRisk = WINNING_MODE_PERCENT; // Aggressive on clear institutional moves
        } else if (Fxausd.currentIntel.institutionalDisplacement < 0.4) {
            baseRisk = LOSING_MODE_PERCENT; // Conservative on low liquidity
        }

        if (history == null || history.isEmpty()) {
            return baseRisk;
        }

        int wins = 0;
        int total = 0;
        int consecutiveLosses = 0;
        int maxConsecutiveLoss = 0;
        double monthlyProfit = 0.0;
        LocalDate now = LocalDate.now(ZoneOffset.UTC);

        for (int i = history.size() - 1; i >= 0; i--) {
            TradeRecord record = history.get(i);
            if (record == null || record.timestamp <= 0) {
                continue;
            }
            LocalDate recordDate = Instant.ofEpochMilli(record.timestamp).atZone(ZoneOffset.UTC).toLocalDate();
            if (recordDate.getMonth() == now.getMonth() && recordDate.getYear() == now.getYear()) {
                monthlyProfit += record.pnlUsd;
            }
            if (record.outcome != null && record.outcome.equals("LOSS")) {
                consecutiveLosses++;
            } else if (record.outcome != null && record.outcome.equals("WIN")) {
                if (consecutiveLosses > maxConsecutiveLoss) {
                    maxConsecutiveLoss = consecutiveLosses;
                }
                consecutiveLosses = 0;
            }
            if (total < 10) {
                if (record.outcome != null && record.outcome.equals("WIN")) {
                    wins++;
                }
                total++;
            }
        }

        double last10WinRate = total > 0 ? (double) wins / total : 0.0;
        if (consecutiveLosses >= 3) {
            return LOSING_MODE_PERCENT;
        }
        if (last10WinRate > 0.65) {
            return WINNING_MODE_PERCENT;
        }
        return NEUTRAL_MODE_PERCENT;
    }

    public boolean shouldPauseTrading(List<TradeRecord> history) {
        if (history == null || history.isEmpty()) {
            return false;
        }

        double startingValue = 0.0;
        double currentValue = 0.0;
        long firstMonth = -1;

        for (TradeRecord record : history) {
            if (record == null) {
                continue;
            }
            if (firstMonth < 0) {
                firstMonth = record.timestamp;
            }
            currentValue += record.pnlUsd;
        }

        if (firstMonth < 0) {
            return false;
        }
        startingValue = 10000.0;
        double drawdown = startingValue <= 0 ? 0.0 : Math.max(0.0, -(currentValue / startingValue));
        return drawdown > DRAWDOWN_PAUSE_THRESHOLD;
    }
}
