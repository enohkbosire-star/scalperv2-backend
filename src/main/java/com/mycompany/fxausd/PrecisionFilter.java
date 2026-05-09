package com.mycompany.fxausd;

import com.mycompany.fxausd.Fxausd.TradeSignal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrecisionFilter {

    public static final double MIN_SIGNAL_STRENGTH = 78.0;
    public static final double MIN_ML_CONFIDENCE = 0.74;
    public static final double DEFAULT_ALLOWED_SPREAD_PIPS = 1.0;
    private static final Set<String> ALLOWED_SESSIONS = Set.of("LONDON", "NY");

    public static List<TradeSignal> filter(List<TradeSignal> candidates, double currentSpreadPips) {
        double allowedSpread = determineAllowedSpread();
        List<TradeSignal> filtered = new ArrayList<>();
        for (TradeSignal signal : candidates) {
            annotateSignal(signal, currentSpreadPips, allowedSpread);
            if (!passesHardFilters(signal, currentSpreadPips, allowedSpread)) {
                continue;
            }
            if (signal.precisionScore < 80) {
                continue;
            }
            filtered.add(signal);
        }
        return filtered;
    }

    public static void annotateSignal(TradeSignal signal, double currentSpreadPips, double allowedSpread) {
        signal.session = determineSession(signal.timestamp);
        signal.trendAligned = isTrendAligned(signal);
        signal.volumeSpike = isVolumeSpike(signal);
        signal.liquiditySweep = isLiquiditySweep(signal);
        signal.BOS = isBreakOfStructure(signal);
        signal.sessionGood = isSessionGood(signal.session);
        signal.lowSpread = isLowSpread(currentSpreadPips, allowedSpread);
        signal.spreadPips = currentSpreadPips;
        signal.setupType = determineSetupType(signal);
        signal.precisionScore = computePrecisionScore(signal);
    }

    public static boolean passesHardFilters(TradeSignal signal, double currentSpreadPips, double allowedSpread) {
        return signal.signalStrength >= MIN_SIGNAL_STRENGTH
                && signal.mlConfidence >= MIN_ML_CONFIDENCE
                && signal.trendAligned
                && currentSpreadPips <= allowedSpread
                && signal.sessionGood
                && signal.volumeSpike;
    }

    public static double determineAllowedSpread() {
        String env = System.getenv("ALLOWED_SPREAD_PIPS");
        if (env == null || env.trim().isEmpty()) {
            return DEFAULT_ALLOWED_SPREAD_PIPS;
        }
        try {
            return Double.parseDouble(env.trim());
        } catch (NumberFormatException ignored) {
            return DEFAULT_ALLOWED_SPREAD_PIPS;
        }
    }

    public static String determineSession(long timestamp) {
        if (timestamp <= 0) {
            return "UNKNOWN";
        }
        ZonedDateTime utc = Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC);
        int hour = utc.getHour();
        if (hour >= 7 && hour < 15) {
            return "LONDON";
        }
        if (hour >= 15 && hour < 21) {
            return "NY";
        }
        return "ASIA";
    }

    public static boolean isTrendAligned(TradeSignal signal) {
        if (signal == null) {
            return false;
        }
        return "trending".equalsIgnoreCase(signal.regime) && signal.signalStrength >= MIN_SIGNAL_STRENGTH;
    }

    public static boolean isVolumeSpike(TradeSignal signal) {
        if (signal == null || signal.features == null) {
            return false;
        }
        int index = getFeatureIndex("volatility");
        if (index >= 0 && index < signal.features.length) {
            return signal.features[index] > 0.75;
        }
        return signal.signalStrength >= 82 && signal.mlConfidence > 0.74;
    }

    public static boolean isLiquiditySweep(TradeSignal signal) {
        if (signal == null) {
            return false;
        }
        return signal.smcConfluence >= 0.70 && signal.signalStrength >= 80;
    }

    public static boolean isBreakOfStructure(TradeSignal signal) {
        if (signal == null) {
            return false;
        }
        return signal.smcConfluence >= 0.70 && signal.riskRewardRatio >= 1.8;
    }

    public static boolean isSessionGood(String session) {
        if (session == null) {
            return false;
        }
        return ALLOWED_SESSIONS.contains(session.toUpperCase());
    }

    public static boolean isLowSpread(double currentSpread, double allowedSpread) {
        return currentSpread <= allowedSpread;
    }

    public static int computePrecisionScore(TradeSignal signal) {
        int score = 0;
        if (signal.trendAligned) {
            score += 25;
        }
        if (signal.liquiditySweep) {
            score += 20;
        }
        if (signal.BOS) {
            score += 20;
        }
        if (signal.mlConfidence > 0.75) {
            score += 20;
        }
        if (signal.sessionGood) {
            score += 10;
        }
        if (signal.lowSpread) {
            score += 5;
        }
        return score;
    }

    public static String determineSetupType(TradeSignal signal) {
        if (signal == null) {
            return "unknown";
        }
        if (signal.liquiditySweep && signal.BOS) {
            return "LiquiditySweep+BOS";
        }
        if (signal.liquiditySweep) {
            return "LiquiditySweep";
        }
        if (signal.BOS) {
            return "BOS";
        }
        if (signal.signalStrength >= 90) {
            return "HighStrength";
        }
        return "A+Setup";
    }

    private static int getFeatureIndex(String featureName) {
        for (int i = 0; i < Fxausd.FEATURE_NAMES.length; i++) {
            if (Fxausd.FEATURE_NAMES[i].equalsIgnoreCase(featureName)) {
                return i;
            }
        }
        return -1;
    }
}
