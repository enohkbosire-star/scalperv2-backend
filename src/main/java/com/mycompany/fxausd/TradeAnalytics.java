package com.mycompany.fxausd;

import com.mycompany.fxausd.Fxausd.TradeRecord;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeAnalytics {

    public static class AnalyticsReport {
        public String summary;
        public String bestSession;
        public String bestPair;
        public String bestSetup;
        public List<String> recommendations = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("📊 Monthly Trade Analytics Report\n");
            builder.append("────────────────────────────────────\n");
            builder.append("Best session: ").append(bestSession).append("\n");
            builder.append("Best pair: ").append(bestPair).append("\n");
            builder.append("Best setup: ").append(bestSetup).append("\n");
            builder.append("Recommendations:\n");
            for (String rec : recommendations) {
                builder.append("  - ").append(rec).append("\n");
            }
            return builder.toString();
        }
    }

    public static AnalyticsReport analyze(List<TradeRecord> history) {
        AnalyticsReport report = new AnalyticsReport();
        if (history == null || history.isEmpty()) {
            report.summary = "No trade history available for analytics.";
            return report;
        }

        Map<String, Double> sessionPnL = new HashMap<>();
        Map<String, Integer> sessionWins = new HashMap<>();
        Map<String, Integer> sessionTotal = new HashMap<>();
        Map<String, Double> pairPnL = new HashMap<>();
        Map<String, Integer> pairWins = new HashMap<>();
        Map<String, Integer> pairTotal = new HashMap<>();
        Map<String, Integer> setupWins = new HashMap<>();
        Map<String, Integer> setupTotal = new HashMap<>();

        for (TradeRecord record : history) {
            if (record == null) {
                continue;
            }
            String session = record.session != null ? record.session : "UNKNOWN";
            String symbol = record.symbol != null ? record.symbol : "UNKNOWN";
            String setup = record.setupType != null ? record.setupType : "unknown";

            sessionPnL.merge(session, record.pnlUsd, Double::sum);
            sessionTotal.merge(session, 1, Integer::sum);
            if ("WIN".equals(record.outcome)) {
                sessionWins.merge(session, 1, Integer::sum);
            }

            pairPnL.merge(symbol, record.pnlUsd, Double::sum);
            pairTotal.merge(symbol, 1, Integer::sum);
            if ("WIN".equals(record.outcome)) {
                pairWins.merge(symbol, 1, Integer::sum);
            }

            setupTotal.merge(setup, 1, Integer::sum);
            if ("WIN".equals(record.outcome)) {
                setupWins.merge(setup, 1, Integer::sum);
            }
        }

        report.bestSession = bestCategory(sessionPnL);
        report.bestPair = bestCategory(pairPnL);
        report.bestSetup = bestCategory(setupWins, setupTotal);

        if (sessionPnL.containsKey("ASIA") && sessionPnL.get("ASIA") < 0) {
            report.recommendations.add("Consider disabling Asia session due to negative performance.");
        }
        if (pairPnL.containsKey("GBPJPY") && pairPnL.get("GBPJPY") < 0) {
            report.recommendations.add("Reduce exposure to GBPJPY due to poor performance.");
        }
        if (report.bestSetup != null && !report.bestSetup.isEmpty()) {
            report.recommendations.add("Prioritize setup: " + report.bestSetup + ".");
        }

        report.summary = String.format("Analytics computed from %d trades.", history.size());
        return report;
    }

    private static String bestCategory(Map<String, Double> aggregatedPnL) {
        if (aggregatedPnL == null || aggregatedPnL.isEmpty()) {
            return "N/A";
        }
        return Collections.max(aggregatedPnL.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private static String bestCategory(Map<String, Integer> wins, Map<String, Integer> total) {
        if (wins == null || total == null || wins.isEmpty() || total.isEmpty()) {
            return "N/A";
        }
        String best = "N/A";
        double bestWinRate = -1.0;
        for (Map.Entry<String, Integer> entry : total.entrySet()) {
            String key = entry.getKey();
            int totalCount = entry.getValue();
            int winCount = wins.getOrDefault(key, 0);
            double winRate = totalCount > 0 ? (double) winCount / totalCount : 0.0;
            if (winRate > bestWinRate) {
                bestWinRate = winRate;
                best = key;
            }
        }
        return best;
    }

    public static void exportMonthlyAnalytics(List<TradeRecord> history, String filename) {
        if (history == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Date,Symbol,Session,SetupType,TrendAligned,Confidence,RR,SpreadPips,Outcome,PNLUSD,Reason\n");
            for (TradeRecord record : history) {
                String date = Instant.ofEpochMilli(record.timestamp).atZone(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
                writer.write(String.format("%s,%s,%s,%s,%b,%.4f,%.2f,%.2f,%s,%.2f,%s\n",
                        date,
                        safe(record.symbol),
                        safe(record.session),
                        safe(record.setupType),
                        record.trendAligned,
                        record.confidence,
                        record.riskRewardRatio,
                        record.spreadPips,
                        safe(record.outcome),
                        record.pnlUsd,
                        safe(record.exitReason)
                ));
            }
        } catch (IOException e) {
            System.out.println("⚠️ Failed to export monthly analytics: " + e.getMessage());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace(",", " ").replace("\n", " ").replace("\r", " ");
    }
}
