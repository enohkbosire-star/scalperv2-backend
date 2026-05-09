package com.mycompany.fxausd;

import java.util.*;

/**
 * ForexBot Controller - Integrates all components
 * Orchestrates signal generation, backtesting, and live trading
 */
public class ForexBotController {
    
    private Fxausd.NaiveBayesClassifier model;
    private List<Fxausd.Candle> candles;
    private ForexBot.BotState botState;
    private ForexBot.Notifier notifier;
    
    public ForexBotController() {
        // Initialize notifier
        notifier = new ForexBot.Notifier(
            System.getenv("TELEGRAM_BOT_TOKEN"),
            System.getenv("TELEGRAM_CHAT_ID"),
            System.getenv("EMAIL_TO")
        );
        
        // Initialize bot state
        botState = new ForexBot.BotState(notifier);
    }
    
    /**
     * Mode 1: Backtest Strategy on Historical Data
     */
    public void runBacktest(String csvFile) throws Exception {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║  🔬 BACKTEST MODE - Historical Analysis       ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
        
        // Load historical data
        candles = Fxausd.loadData(csvFile);
        System.out.println("✅ Loaded " + candles.size() + " candles from: " + csvFile);
        
        // Prepare dataset
        List<double[]> features = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();
        
        for (int i = 50; i < candles.size() - 10; i++) {
            double[] f = Fxausd.buildFeatures(candles, i);
            int label = Fxausd.createLabel(candles, i);
            features.add(f);
            labels.add(label);
        }
        
        System.out.println("✅ Dataset: " + features.size() + " samples prepared\n");
        
        // Train model with normalized features and pass scaler into backtest
        Fxausd.FeatureScaler scaler = new Fxausd.FeatureScaler(11);
        java.util.List<double[]> scaledFeatures = scaler.fitTransform(features);
        model = new Fxausd.NaiveBayesClassifier(11);
        model.train(scaledFeatures, labels);
        System.out.println("✅ Model trained on historical data\n");
        
        // Run walk-forward backtest with realistic sizing and past-only training
        Backtester.BacktestResult result = Backtester.runBacktest(candles, 80, 16);
        System.out.println(result.toString());
        
        // Save results
        saveBacktestReport(result);
    }
    
    /**
     * Mode 2: Live Trading with MT5 Integration
     */
    public void runLiveTrading() throws Exception {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║  🤖 LIVE TRADING MODE - MT5 Integration       ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
        
        // Start the REST API server for MT5
        ForexBot.startAPIServer(botState);
        
        // Load training data once
        candles = Fxausd.loadData("data/eurusd.csv");
        
        // Train model
        List<double[]> features = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();
        
        for (int i = 50; i < candles.size() - 10; i++) {
            double[] f = Fxausd.buildFeatures(candles, i);
            int label = Fxausd.createLabel(candles, i);
            features.add(f);
            labels.add(label);
        }
        
        model = new Fxausd.NaiveBayesClassifier(11);
        model.train(features, labels);
        
        System.out.println("✅ Model trained and ready for live trading\n");
        
        // Keep alive
        while (true) {
            Thread.sleep(5000);
            
            // Periodic status updates
            printBotStatus();
        }
    }
    
    /**
     * Mode 3: Signal Generation Only (UI Integration)
     */
    public void generateSignals(String csvFile) throws Exception {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║  📊 SIGNAL GENERATION MODE - Pure Signals     ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
        
        candles = Fxausd.loadData(csvFile);
        
        List<double[]> features = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();
        
        for (int i = 50; i < candles.size() - 10; i++) {
            double[] f = Fxausd.buildFeatures(candles, i);
            int label = Fxausd.createLabel(candles, i);
            features.add(f);
            labels.add(label);
        }
        
        model = new Fxausd.NaiveBayesClassifier(11);
        model.train(features, labels);
        
        // Generate signals for recent data
        System.out.println("📈 Generating Trading Signals:\n");
        
        int trainSize = (int) (features.size() * 0.7);
        List<Fxausd.TradeSignal> allSignals = new ArrayList<>();
        
        for (int i = Math.max(0, features.size() - 10); i < features.size(); i++) {
            int pred = model.predict(features.get(i));
            double prob = model.predictProbability(features.get(i));
            int originalIndex = 50 + trainSize + i;
            
            if (originalIndex >= candles.size() || originalIndex < 20) continue;
            
            // Generate SMC signal
            Fxausd.SMCSignal smcSignal = Fxausd.generateSMCSignal(candles, originalIndex, pred, prob);
            
            // Calculate strength
            double strength = calculateSignalStrengthWrapper(candles, originalIndex, pred, prob, smcSignal.confidence);
            
            double risk = Math.abs(smcSignal.entry - smcSignal.stopLoss);
            double reward = Math.abs(smcSignal.takeProfit - smcSignal.entry);
            
            Fxausd.TradeSignal signal = new Fxausd.TradeSignal(
                smcSignal.direction, smcSignal.entry, smcSignal.stopLoss, smcSignal.takeProfit,
                prob, smcSignal.confidence, strength, smcSignal.smcReason, risk, reward
            );
            allSignals.add(signal);
            
            // Display signal
            System.out.printf("🎯 Signal #%d: %s\n", allSignals.size(), smcSignal.direction);
            System.out.printf("   Entry: %.4f | SL: %.4f | TP: %.4f\n", signal.entry, signal.stopLoss, signal.takeProfit);
            System.out.printf("   Confidence: %.2f%% | Strength: %.2f%%\n", prob * 100, strength);
            System.out.printf("   Risk/Reward: %.2f:1\n\n", signal.riskRewardRatio);
        }
        
        // Export signals
        Fxausd.exportSignalsToCSV(allSignals, "forex_signals_" + 
            System.currentTimeMillis() + ".csv");
    }
    
    // Wrapper for signal strength calculation
    private double calculateSignalStrengthWrapper(List<Fxausd.Candle> data, int index, 
                                                  int pred, double mlConf, double smcConf) {
        return Fxausd.calculateSignalStrength(data, index, pred, mlConf, smcConf);
    }
    
    /**
     * Print bot status
     */
    private void printBotStatus() {
        System.out.println("\n┌─────────────────────────────────────┐");
        System.out.println("│ 📊 Bot Status - " + 
            new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) + " │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│ 💰 Balance: $" + String.format("%.2f", botState.accountBalance));
        System.out.println("│ 📈 Total Trades: " + botState.totalTrades);
        System.out.println("│ ✅ Wins: " + botState.winTrades);
        System.out.println("│ 🎯 Win Rate: " + String.format("%.2f%%", botState.getWinRate()));
        System.out.println("│ 📊 Profit Factor: " + String.format("%.2f", botState.getProfitFactor()));
        System.out.println("│ 🔄 Open Positions: " + botState.openPositions.size());
        System.out.println("│ 📋 Pending Signals: " + botState.signalQueue.size());
        System.out.println("└─────────────────────────────────────┘");
    }
    
    /**
     * Save backtest report to file
     */
    private void saveBacktestReport(Backtester.BacktestResult result) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(
            new java.io.FileWriter("backtest_report_" + System.currentTimeMillis() + ".txt"))) {
            writer.println(result.toString());
            System.out.println("📄 Report saved to: backtest_report.txt");
        } catch (Exception e) {
            System.out.println("Error saving report: " + e.getMessage());
        }
    }
    
    /**
     * Main - Select operating mode
     */
    public static void main(String[] args) {
        ForexBotController controller = new ForexBotController();
        
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║           🤖 ForexBot v1.0                       ║");
        System.out.println("║  ML-Powered Trading Bot with SMC Strategy        ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        
        System.out.println("\n📋 Select Operating Mode:");
        System.out.println("  1. 🔬 Backtest - Test strategy on historical data");
        System.out.println("  2. 🤖 Live Trading - Connect to MT5 and trade live");
        System.out.println("  3. 📊 Signal Generation - Generate signals only");
        System.out.println("  4. ⚙️  Configuration Guide");
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nEnter mode (1-4): ");
        
        try {
            int mode = scanner.nextInt();
            
            switch (mode) {
                case 1:
                    controller.runBacktest("data/eurusd.csv");
                    break;
                case 2:
                    controller.runLiveTrading();
                    break;
                case 3:
                    controller.generateSignals("data/eurusd.csv");
                    break;
                case 4:
                    printConfigurationGuide();
                    break;
                default:
                    System.out.println("Invalid mode");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    /**
     * Print setup guide
     */
    private static void printConfigurationGuide() {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║       🔧 ForexBot Configuration Guide             ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");
        
        System.out.println("1️⃣  SET UP ENVIRONMENT VARIABLES:");
        System.out.println("   Windows: setx TELEGRAM_BOT_TOKEN \"your_token\"");
        System.out.println("   Windows: setx TELEGRAM_CHAT_ID \"your_chat_id\"");
        System.out.println("   Linux: export TELEGRAM_BOT_TOKEN=\"your_token\"");
        System.out.println("   Get tokens from: https://t.me/BotFather\n");
        
        System.out.println("2️⃣  SETUP MT5 EXPERT ADVISOR:");
        System.out.println("   • Copy ForexBot_MT5_EA.mq4 to: MT5/experts/ folder");
        System.out.println("   • Compile in MetaEditor");
        System.out.println("   • Attach to chart");
        System.out.println("   • Set Bot Server IP: localhost or your server IP");
        System.out.println("   • Set Bot API Key in EA (must match Java side)\n");
        
        System.out.println("3️⃣  RUN JAVA BOT SERVER:");
        System.out.println("   java -cp . ForexBotController");
        System.out.println("   Select Mode 2: Live Trading\n");
        
        System.out.println("4️⃣  VERIFY CONNECTION:");
        System.out.println("   • Check API Dashboard: http://localhost:8888/api/dashboard");
        System.out.println("   • Check Telegram for alerts\n");
        
        System.out.println("5️⃣  RISK MANAGEMENT:");
        System.out.println("   • Start with demo account first");
        System.out.println("   • Risk max 2% per trade");
        System.out.println("   • Monitor bot status regularly\n");
    }
}


