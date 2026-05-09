# ForexBot Configuration Template

## 🔧 Environment Variables

Set these on your system:

```batch
# Windows Command Prompt (Run as Administrator)
setx TELEGRAM_BOT_TOKEN "YOUR_BOT_TOKEN_HERE"
setx TELEGRAM_CHAT_ID "YOUR_CHAT_ID_HERE"
setx EMAIL_TO "your.email@gmail.com"
setx LOG_LEVEL "INFO"
```

```bash
# Linux/Mac Terminal
export TELEGRAM_BOT_TOKEN="YOUR_BOT_TOKEN_HERE"
export TELEGRAM_CHAT_ID="YOUR_CHAT_ID_HERE"
export EMAIL_TO="your.email@gmail.com"
export LOG_LEVEL="INFO"
```

---

## 🤖 Java Bot Configuration

Edit: `ForexBotController.java`

```java
// REST API Server Settings
private static final int PORT = 8888;
private static final String API_KEY = "forex_bot_secret_key_12345";

// Account Settings
double accountBalance = 10000;        // Starting balance in USD
double riskPerTrade = 0.02;           // 2% risk per trade
int maxConcurrentTrades = 5;

// Trade Settings
double stopLossMultiplier = 1.5;      // 1.5 × ATR
double takeProfitMultiplier = 3.0;    // 3.0 × ATR
double trailingStopMultiplier = 0.5;  // 50% of SL distance
```

---

## 🤖 MT5 Expert Advisor Configuration

Edit in MetaTrader 5 after attaching EA:

```
Inputs Tab:
├─ BotServerIP: "localhost"          (or your server IP)
├─ BotServerPort: 8888               (REST API port)
├─ BotAPIKey: "forex_bot_secret_key_12345"
├─ SymbolToTrade: "EURUSD"
└─ SignalUpdateInterval: 60           (seconds between signals)

Indicator Settings:
├─ RSIPeriod: 14
├─ MA20Period: 20
├─ MA50Period: 50
├─ ATRPeriod: 14
└─ BollingerPeriod: 20
```

---

## 📊 Indicator Parameters (Fine Tuning)

### RSI Settings
```
Period: 14
Oversold: < 30
Overbought: > 70
Sensitivity: High = 14, Medium = 20, Low = 7
```

### Moving Averages
```
Short MA: 20  (faster, more signals)
Long MA: 50   (slower, fewer signals)
Smoothing: SMA (default), EMA (optional)
```

### ATR (Volatility)
```
Period: 14 (standard)
SL Distance: 1.5 × ATR
TP Distance: 3.0 × ATR

For Scalping:    1.0 × ATR SL, 2.0 × ATR TP
For Swing:       2.0 × ATR SL, 4.0 × ATR TP
For Day Trade:   1.5 × ATR SL, 3.0 × ATR TP (default)
```

### Bollinger Bands
```
Period: 20
Standard Deviations: 2.0
Price: Close

Entry Logic:
- BUY: Above lower band + RSI < 30
- SELL: Below upper band + RSI > 70
```

---

## 🎯 Trading Rules Configuration

### Signal Strength Thresholds
```
Weak:   < 60%  → Don't trade
Medium: 60-75% → Trade with caution
Strong: > 75%  → Full position size
```

### Risk Management Rules
```
Account Size: $10,000
Risk Per Trade: 2% = $200
Stop Loss: 1.5 × ATR
Take Profit: 3.0 × ATR
Risk/Reward: Minimum 1:1.5

Position Size Calculation:
LotSize = (Risk in $) / (Risk in Pips × Pip Value)
```

### News Filter Calendar
```
Avoid Trading 1 Hour Before/After:
- US NFP (First Friday, 13:30 UTC)
- ECB Rate Decision (Quarterly)
- FOMC Meeting (8x per year)
- Non-Farm Payroll
- CPI Release
- GDP Announcement
```

---

## 📊 Backtesting Parameters

### Data Requirements
```
Minimum:    500 candles (20 days on 1H)
Good:       1000 candles (40 days)
Excellent:  2000+ candles (80+ days)
```

### Quality Checks
```
✓ No gaps in data
✓ Consistent timeframe (1H recommended)
✓ High/Low > Low/High (logical order)
✓ Volume data (optional but recommended)
```

### Expected Results
```
With Adequate Data:
- Win Rate: 60-70%
- Profit Factor: 1.5-2.5
- Sharpe Ratio: 1.0-2.0
- Max Drawdown: 10-20%

Poor Results Indicate:
- Insufficient data
- Market conditions changed
- Parameters need adjustment
```

---

## 🚨 Risk Management Profiles

### Conservative (Low Risk)
```
Account Risk: 1% per trade
Position Size: 0.1 lots
SL Distance: 2.0 × ATR
TP Distance: 2.0 × ATR (1:1 RR)
Max Drawdown: 5%
Best For: Beginners, small accounts
```

### Balanced (Medium Risk) [DEFAULT]
```
Account Risk: 2% per trade
Position Size: 0.2-0.5 lots
SL Distance: 1.5 × ATR
TP Distance: 3.0 × ATR (1:2 RR)
Max Drawdown: 15%
Best For: Standard trading
```

### Aggressive (High Risk)
```
Account Risk: 3-5% per trade
Position Size: 1.0+ lots
SL Distance: 1.0 × ATR
TP Distance: 3.0 × ATR (1:3 RR)
Max Drawdown: 25%
Best For: Experienced traders, large accounts
```

---

## 🔐 Production Checklist

### Pre-Deployment
- [ ] Backtest on 1000+ candles (≥65% win rate)
- [ ] Run on demo account for 1 week minimum
- [ ] Verify all alerts work (Telegram/Email)
- [ ] Check API connectivity (dashboard loads)
- [ ] Confirm position sizing is correct
- [ ] Set stop loss on all positions
- [ ] Backup configuration files
- [ ] Monitor system resources

### During Live Trading
- [ ] Check dashboard daily
- [ ] Monitor P&L trends
- [ ] Verify alerts are sent
- [ ] Review closed trades
- [ ] Keep position size small initially
- [ ] Don't change settings during trade
- [ ] Scale up gradually (increase lot size)

### Monitoring Points
```
Daily: P&L, position status, alerts
Weekly: Win rate, profit factor, drawdown
Monthly: Performance report, strategy review
```

---

## 🔧 Advanced Customization

### Custom Indicator Combinations
```java
// In Fxausd.java buildFeatures()

// Example: Add custom indicator
double custom = calculateCustomIndicator(data, index);
// Then add to features array
return new double[] {
    close, rsi, ma20, ma50, momentum, 
    volatility, macd, bbPercent, stochK, 
    atr, cci, custom  // Add your indicator
};
```

### Custom Entry Signals
```java
// In ForexBot_MT5_EA.mq5 OnTick()

// Add your own condition
if (customCondition1 && customCondition2 && rsi < 30) {
    direction = "BUY";
    reason = "My Custom Signal Logic";
    confidence = 0.80;
}
```

### Multi-Pair Trading
```java
// Currently: Single pair (EURUSD)
// To add multiple pairs:

String[] tradingPairs = {"EURUSD", "GBPUSD", "USDJPY"};
for (String pair : tradingPairs) {
    // Generate signals for each pair
}
```

---

## 📈 Performance Optimization

### For Speed
```
• Use 1H timeframe (faster signals)
• Reduce backtesting data (but keep 500+ candles)
• Increase signal update interval (60+ seconds)
• Run on local server vs cloud
```

### For Accuracy
```
• Use more candles (1000+)
• Combine multiple timeframes
• Add more indicators
• Use longer lookback periods
```

### For Stability
```
• Increase news filter buffer (2 hours vs 1 hour)
• Reduce max concurrent trades (3 vs 5)
• Lower risk per trade (1% vs 2%)
• Increase SL distance (2.0 × ATR vs 1.5)
```

---

## 🐛 Debug Configuration

### Enable Verbose Logging
```bash
java -Dlog.level=DEBUG -cp . ForexBotController
```

### MT5 Debugging
```
In MT5 Console (Ctrl+2):
- Check Expert Advisor logs
- Look for HTTP errors
- Verify indicator calculations
- Test with Print() statements
```

### Java Debugging
```
Add to ForexBotController.java:
System.out.println("DEBUG: " + variableName);
```

---

## 📋 Troubleshooting Configuration

### Problem: Bot generates too many signals
**Solution:**
```
• Increase confidence threshold (65% → 75%)
• Increase signal interval (60s → 120s)
• Add more SMC confluence requirements
```

### Problem: Bot generates too few signals
**Solution:**
```
• Decrease confidence threshold (75% → 60%)
• Decrease signal interval (120s → 60s)
• Relax indicator thresholds
```

### Problem: High losses
**Solution:**
```
• Increase SL distance (1.5 × ATR → 2.0 × ATR)
• Decrease position size (0.5 → 0.2)
• Add news filter buffer
• Increase confidence requirement
```

### Problem: Hits maximum drawdown
**Solution:**
```
• Reduce position size by 50%
• Increase SL distance
• Enable pause on drawdown
• Reduce risk per trade (2% → 1%)
```

---

## 🎯 Sample Configurations

### Beginners
```
Risk Per Trade: 1%
Position Size: 0.1 lots
SL: 2.0 × ATR
TP: 2.0 × ATR
Confidence: 75%
Backtest: 1000 candles
```

### Professionals
```
Risk Per Trade: 2-3%
Position Size: 0.5-1.0 lots
SL: 1.5 × ATR
TP: 3.0 × ATR
Confidence: 65%
Backtest: 2000+ candles
```

### Scalpers (Short-term)
```
Risk Per Trade: 0.5%
Position Size: 0.05 lots
SL: 0.8 × ATR
TP: 1.5 × ATR
Timeframe: 15M
Confidence: 70%
```

---

## 📞 Configuration Support

For issues with configuration:
1. Check backtest results first
2. Review logs (Java console)
3. Compare with sample configs
4. Start with conservative settings
5. Adjust one parameter at a time
6. Re-backtest after each change

---

**Configuration Version:** 1.0  
**Last Updated:** April 2026  
**Bot Version:** ForexBot v1.0


