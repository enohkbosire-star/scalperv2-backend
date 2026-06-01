package com.mycompany.fxausd;

import java.io.*;
import java.util.*;

public class Backtester {
    
    public static class BacktestResult {
        public int totalTrades;
        public int winTrades;
        public int lossTrades;
        public double winRate;
        public double grossProfit;
        public double grossLoss;
        public double netProfit;
        public double grossProfitUsd;
        public double grossLossUsd;
        public double netProfitUsd;
        public double startingBalanceUsd = 10000.0;
        public double endingBalanceUsd;
        public java.util.List<Double> equityCurveUsd = new ArrayList<>();
        public double averageWin;
        public double averageLoss;
        public double averageWinUsd;
        public double averageLossUsd;
        public double profitFactor;
        public double maxDrawdown;
        public double sharpeRatio;
        public double riskRewardRatio;
        public double expectancy;
        
        public void calculate(java.util.List<Trade> trades) {
            totalTrades = trades.size();
            winTrades = (int) trades.stream().filter(t -> t.profit > 0).count();
            lossTrades = totalTrades - winTrades;
            winRate = totalTrades > 0 ? (winTrades * 100.0 / totalTrades) : 0;
            
            grossProfit = trades.stream().filter(t -> t.profit > 0).mapToDouble(t -> t.profit).sum();
            grossLoss = Math.abs(trades.stream().filter(t -> t.profit < 0).mapToDouble(t -> t.profit).sum());
            netProfit = grossProfit - grossLoss;
            grossProfitUsd = trades.stream().filter(t -> t.profitUsd > 0).mapToDouble(t -> t.profitUsd).sum();
            grossLossUsd = Math.abs(trades.stream().filter(t -> t.profitUsd < 0).mapToDouble(t -> t.profitUsd).sum());
            netProfitUsd = grossProfitUsd - grossLossUsd;
            endingBalanceUsd = startingBalanceUsd + netProfitUsd;
            profitFactor = grossLoss > 0 ? grossProfit / grossLoss : 0;
            
            averageWin = winTrades > 0 ? grossProfit / winTrades : 0;
            averageLoss = lossTrades > 0 ? grossLoss / lossTrades : 0;
            averageWinUsd = winTrades > 0 ? grossProfitUsd / winTrades : 0;
            averageLossUsd = lossTrades > 0 ? grossLossUsd / lossTrades : 0;
            riskRewardRatio = averageLoss > 0 ? averageWin / averageLoss : 0;
            expectancy = totalTrades > 0 ? ((grossProfit - grossLoss) / totalTrades) : 0;
            
            calculateDrawdown(trades);
            calculateSharpeRatio(trades);
        }
        
        private void calculateDrawdown(java.util.List<Trade> trades) {
            double peak = 0;
            double maxDD = 0;
            double equity = 0;
            
            for (Trade t : trades) {
                equity += t.profit;
                peak = Math.max(peak, equity);
                double dd = peak > 0 ? (peak - equity) / peak : 0;
                maxDD = Math.max(maxDD, dd);
            }
            
            maxDrawdown = maxDD * 100;
        }
        
        private void calculateSharpeRatio(java.util.List<Trade> trades) {
            if (trades.size() < 2) {
                sharpeRatio = 0;
                return;
            }
            
            double[] returns = new double[trades.size()];
            for (int i = 0; i < trades.size(); i++) {
                returns[i] = trades.get(i).profit;
            }
            
            double mean = Arrays.stream(returns).average().orElse(0);
            double variance = Arrays.stream(returns)
                .map(x -> Math.pow(x - mean, 2))
                .average()
                .orElse(0);
            double stdDev = Math.sqrt(variance);
            
            sharpeRatio = stdDev > 0 ? (mean / stdDev) * Math.sqrt(252) : 0; // Annualized
        }
        
        @Override
        public String toString() {
            return String.format(
                "\n════════════════════════════════════════════════\n" +
                "📊 BACKTEST RESULTS\n" +
                "════════════════════════════════════════════════\n" +
                "Total Trades: %d | Wins: %d | Losses: %d\n" +
                "Win Rate: %.2f%% | Profit Factor: %.2f\n" +
                "Net Profit: %.1f pips / $%.2f | Gross Profit: %.1f pips / $%.2f | Gross Loss: %.1f pips / $%.2f\n" +
                "Avg Win: %.1f pips / $%.2f | Avg Loss: %.1f pips / $%.2f | Risk/Reward: %.2f:1 | Expectancy: $%.2f\n" +
                "Max Drawdown: %.2f%% | Sharpe Ratio: %.2f\n" +
                "════════════════════════════════════════════════\n",
                totalTrades, winTrades, lossTrades, winRate, profitFactor,
                netProfit, netProfitUsd, grossProfit, grossProfitUsd, grossLoss, grossLossUsd,
                averageWin, averageWinUsd, averageLoss, averageLossUsd,
                riskRewardRatio, expectancy, maxDrawdown, sharpeRatio
            );
        }
    }
    
    public static class Trade {
        public int id;
        public long openTime;
        public long closeTime;
        public String symbol;
        public String direction;
        public double entryPrice;
        public double exitPrice;
        public double stopLoss;
        public double takeProfit;
        public double profit; // in pips
        public String exitReason;
        
        public int entryIndex;

        public Trade(int i, String sym, String dir, double entry, double sl, double tp, double lotSize, int index) {
            id = i;
            symbol = sym;
            direction = dir;
            entryPrice = entry;
            stopLoss = sl;
            takeProfit = tp;
            this.lotSize = lotSize;
            entryIndex = index;
            openTime = System.currentTimeMillis();
        }
        
        public double lotSize;
        public double profitUsd;

        public boolean exit(double price, String reason) {
            if (direction.equals("BUY")) {
                if (price <= stopLoss) {
                    exitPrice = stopLoss;
                    profit = (stopLoss - entryPrice) * 10000;
                    exitReason = reason;
                    profitUsd = profit * lotSize * 10.0;
                    return true;
                } else if (price >= takeProfit) {
                    exitPrice = takeProfit;
                    profit = (takeProfit - entryPrice) * 10000;
                    exitReason = reason;
                    profitUsd = profit * lotSize * 10.0;
                    return true;
                }
            } else {
                if (price >= stopLoss) {
                    exitPrice = stopLoss;
                    profit = (entryPrice - stopLoss) * 10000;
                    exitReason = reason;
                    profitUsd = profit * lotSize * 10.0;
                    return true;
                } else if (price <= takeProfit) {
                    exitPrice = takeProfit;
                    profit = (entryPrice - takeProfit) * 10000;
                    exitReason = reason;
                    profitUsd = profit * lotSize * 10.0;
                    return true;
                }
            }
            return false;
        }

        public boolean evaluateBar(Fxausd.Candle candle) {
            double barOpen = candle.open;
            double barHigh = candle.high;
            double barLow = candle.low;

            if (direction.equals("BUY")) {
                if (barLow <= stopLoss && barHigh >= takeProfit) {
                    double distSL = Math.abs(barOpen - stopLoss);
                    double distTP = Math.abs(takeProfit - barOpen);
                    if (distSL <= distTP) {
                        return exit(stopLoss, "Stop Loss Hit");
                    }
                    return exit(takeProfit, "Take Profit Hit");
                } else if (barLow <= stopLoss) {
                    return exit(stopLoss, "Stop Loss Hit");
                } else if (barHigh >= takeProfit) {
                    return exit(takeProfit, "Take Profit Hit");
                }
            } else {
                if (barLow <= takeProfit && barHigh >= stopLoss) {
                    double distSL = Math.abs(barOpen - stopLoss);
                    double distTP = Math.abs(barOpen - takeProfit);
                    if (distSL <= distTP) {
                        return exit(stopLoss, "Stop Loss Hit");
                    }
                    return exit(takeProfit, "Take Profit Hit");
                } else if (barHigh >= stopLoss) {
                    return exit(stopLoss, "Stop Loss Hit");
                } else if (barLow <= takeProfit) {
                    return exit(takeProfit, "Take Profit Hit");
                }
            }
            return false;
        }
    }
    
    public static BacktestResult runBacktest(java.util.List<Fxausd.Candle> data,
                                             int numTrees,
                                             int numFeatures) {
        java.util.List<Trade> trades = new ArrayList<>();
        java.util.List<Trade> activeTrades = new ArrayList<>();
        java.util.List<Double> equityCurveUsd = new ArrayList<>();
        double accountBalanceUsd = 10000.0;
        double balanceUsd = accountBalanceUsd;
        double riskPerTradeUsd = accountBalanceUsd * 0.02;
        int tradeId = 0;
        int maxOpenTrades = 1;

        double spreadPips = 1.0;
        double slippagePips = 0.2;
        double commissionPips = 0.5;
        double totalCostPips = spreadPips + slippagePips + commissionPips;
        double spreadPrice = spreadPips * 0.0001;
        double slippagePrice = slippagePips * 0.0001;
        double pipValuePerStandardLot = 10.0;
        int retrainInterval = 50;
        Fxausd.RandomForestClassifier backtestModel = null;
        Fxausd.FeatureScaler backtestScaler = null;

        System.out.println("\n🔬 Running Walk-Forward Backtest on " + data.size() + " candles...\n");

        for (int i = 51; i < data.size() - 1; i++) {
            Fxausd.Candle candle = data.get(i);

            // Evaluate open trades first
            Iterator<Trade> iterator = activeTrades.iterator();
            while (iterator.hasNext()) {
                Trade trade = iterator.next();
                if (trade.evaluateBar(candle)) {
                    trade.profit -= totalCostPips;
                    trade.profitUsd = trade.profit * trade.lotSize * pipValuePerStandardLot;
                    balanceUsd += trade.profitUsd;
                    equityCurveUsd.add(balanceUsd);
                    System.out.println("Trade #" + trade.id + " CLOSED | P&L: " + 
                                     String.format("%.1f", trade.profit) + " pips | " + trade.exitReason);
                    trades.add(trade);
                    iterator.remove();
                }
            }

            if (activeTrades.size() >= maxOpenTrades) {
                continue;
            }

            java.util.List<double[]> trainFeatures = new ArrayList<>();
            java.util.List<Integer> trainLabels = new ArrayList<>();
            for (int j = 50; j < i && j < data.size() - 10; j++) {
                double[] featureRow = Fxausd.buildFeatures(data, j);
                int label = Fxausd.createLabel(data, j);
                if (label != -1) {
                    trainFeatures.add(featureRow);
                    trainLabels.add(label);
                }
            }
            if (trainFeatures.size() < 10) {
                continue;
            }

            if (i == 51 || i % retrainInterval == 0 || backtestModel == null) {
                backtestScaler = new Fxausd.FeatureScaler(numFeatures);
                java.util.List<double[]> scaledTrainFeatures = backtestScaler.fitTransform(trainFeatures);
                backtestModel = new Fxausd.RandomForestClassifier(numTrees, numFeatures);
                backtestModel.train(scaledTrainFeatures, trainLabels);
            }

            if (backtestModel == null || backtestScaler == null) {
                continue;
            }

            double[] featureRow = backtestScaler.transform(Fxausd.buildFeatures(data, i));
            double probability = backtestModel.predictProbability(featureRow);
            int prediction;
            if (probability > 0.65) {
                prediction = 1;
            } else if (probability < 0.35) {
                prediction = 0;
            } else {
                continue;
            }

            if ("RANGING".equalsIgnoreCase(Fxausd.detectMarketRegime(data, i, 20))) {
                continue;
            }

            Fxausd.SMCSignal smcSignal = Fxausd.generateSMCSignal(data, i, prediction, probability);
            double strength = Fxausd.calculateSignalStrength(data, i, prediction, probability, smcSignal.confidence);
            double riskPips = Math.abs(smcSignal.entry - smcSignal.stopLoss) / 0.0001;
            double rewardPips = Math.abs(smcSignal.takeProfit - smcSignal.entry) / 0.0001;
            double effectiveRiskPips = riskPips + totalCostPips;
            double effectiveRewardPips = rewardPips - totalCostPips;
            
            if (probability < 0.60 || strength < 60 || effectiveRiskPips <= 0 || effectiveRewardPips <= 0 || effectiveRewardPips / effectiveRiskPips < 1.5) {
                continue;
            }

            if (activeTrades.size() >= maxOpenTrades) {
                continue;
            }

            double lotSize = riskPerTradeUsd / (riskPips * pipValuePerStandardLot);
            if (lotSize <= 0) {
                continue;
            }

            Fxausd.Candle entryCandle = data.get(i + 1);
            double entryPrice = entryCandle.open + (prediction == 1 ? spreadPrice + slippagePrice : -spreadPrice - slippagePrice);
            double shift = entryPrice - smcSignal.entry;
            double stopLoss = smcSignal.stopLoss + shift;
            double takeProfit = smcSignal.takeProfit + shift;

            if (prediction == 1 && stopLoss >= entryPrice) continue;
            if (prediction == 0 && stopLoss <= entryPrice) continue;

            Trade trade = new Trade(++tradeId, "EURUSD", smcSignal.direction, entryPrice, stopLoss, takeProfit, lotSize, i + 1);
            activeTrades.add(trade);
            System.out.println("Trade #" + tradeId + " OPENED | " + smcSignal.direction + 
                             " @ " + String.format("%.5f", entryPrice) + " | Prob: " + String.format("%.2f", probability));
        }

        Fxausd.Candle lastCandle = data.get(data.size() - 1);
        for (Trade trade : activeTrades) {
            double pips = simulateTradePips(data, trade.entryIndex, trade.entryPrice, trade.stopLoss, trade.takeProfit, Math.max(1, data.size() - trade.entryIndex - 1));
            trade.profit = pips - totalCostPips;
            trade.profitUsd = trade.profit * trade.lotSize * pipValuePerStandardLot;
            trade.exitReason = "Closed at end of data";
            balanceUsd += trade.profitUsd;
            equityCurveUsd.add(balanceUsd);
            trades.add(trade);
        }

        if (equityCurveUsd.isEmpty()) {
            equityCurveUsd.add(balanceUsd);
        }

        BacktestResult result = new BacktestResult();
        result.equityCurveUsd = equityCurveUsd;
        result.calculate(trades);
        return result;
    }

    public static double simulateTradePips(java.util.List<Fxausd.Candle> data,
                                           int entryIndex,
                                           double entryPrice,
                                           double stopLoss,
                                           double takeProfit,
                                           int maxBars) {
        if (entryIndex >= data.size()) return 0;

        for (int j = entryIndex; j < data.size() && j < entryIndex + maxBars; j++) {
            Fxausd.Candle candle = data.get(j);
            double high = candle.high;
            double low = candle.low;

            if (takeProfit > entryPrice && stopLoss < entryPrice) {
                if (low <= stopLoss && high >= takeProfit) {
                    double distSL = Math.abs(entryPrice - stopLoss);
                    double distTP = Math.abs(takeProfit - entryPrice);
                    return distSL <= distTP ? toPips(stopLoss - entryPrice) : toPips(takeProfit - entryPrice);
                } else if (low <= stopLoss) {
                    return toPips(stopLoss - entryPrice);
                } else if (high >= takeProfit) {
                    return toPips(takeProfit - entryPrice);
                }
            } else if (takeProfit < entryPrice && stopLoss > entryPrice) {
                if (high >= stopLoss && low <= takeProfit) {
                    double distSL = Math.abs(stopLoss - entryPrice);
                    double distTP = Math.abs(entryPrice - takeProfit);
                    return distSL <= distTP ? toPips(stopLoss - entryPrice) : toPips(entryPrice - takeProfit);
                } else if (high >= stopLoss) {
                    return toPips(stopLoss - entryPrice);
                } else if (low <= takeProfit) {
                    return toPips(entryPrice - takeProfit);
                }
            }
        }

        Fxausd.Candle last = data.get(Math.min(data.size() - 1, entryIndex + maxBars - 1));
        double exitPrice = last.close;
        if (entryPrice < exitPrice) {
            return toPips(exitPrice - entryPrice);
        }
        return toPips(entryPrice - exitPrice);
    }

    private static double toPips(double priceDifference) {
        return priceDifference / 0.0001;
    }
}

