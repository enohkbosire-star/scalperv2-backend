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
     * Live Trading with MT5 Integration
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
        
        model = new Fxausd.NaiveBayesClassifier(Fxausd.NUM_FEATURES);
        model.train(features, labels);
        
        System.out.println("✅ Model trained and ready for live trading\n");
        
        // Keep alive
        while (true) {
            Thread.sleep(5000);
            
            // Periodic status updates
            printBotStatus();
        }
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
     * Main - Select operating mode
     */
    public static void main(String[] args) {
        ForexBotController controller = new ForexBotController();
        
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║           🤖 ForexBot v1.0                       ║");
        System.out.println("║  ML-Powered Trading Bot with SMC Strategy        ║");
        System.out.println("║         (LIVE TRADING ONLY MODE)                 ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        
        System.out.println("\n🚀 Starting Live Trading Mode...");
        System.out.println("Connecting to MT5 and preparing ML models...");
        
        try {
            controller.runLiveTrading();
        } catch (Exception e) {
            System.out.println("Critical Error in Live Trading: " + e.getMessage());
            e.printStackTrace();
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
        System.out.println("   Live Trading starts automatically.\n");
        
        System.out.println("4️⃣  VERIFY CONNECTION:");
        System.out.println("   • Check API Dashboard: http://localhost:8888/api/dashboard");
        System.out.println("   • Check Telegram for alerts\n");
        
        System.out.println("5️⃣  RISK MANAGEMENT:");
        System.out.println("   • Start with demo account first");
        System.out.println("   • Risk max 2% per trade");
        System.out.println("   • Monitor bot status regularly\n");
    }
}


