package com.mycompany.fxausd;

public class TestMobileBridge {
    public static void main(String[] args) {
        System.out.println("🚀 Starting Mobile Bridge Test...");
        
        // Simulating a bot signal
        String symbol = "EURUSD";
        String action = "BUY";
        double entry = 1.12345;
        double tp = 1.13000;
        double sl = 1.12000;
        
        System.out.println("📨 Sending test signal to mobile app...");
        MobileSignalBridge.sendToMobile(symbol, action, entry, tp, sl);
        
        System.out.println("✅ Test triggered! Check your Android app dashboard in a few seconds.");
        System.out.println("(Make sure your Spark API is running in VS Code)");
    }
}
