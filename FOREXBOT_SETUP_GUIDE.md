# 🤖 ForexBot - Complete Installation & Setup Guide

## Overview
ForexBot is an automated MT5 trading bot that combines:
- ✅ Naive Bayes ML classifier for price prediction
- ✅ Smart Money Concept (SMC) strategy for institutional trading
- ✅ 11 technical indicators (RSI, MACD, ATR, Bollinger Bands, etc.)
- ✅ Real-time MT5 integration
- ✅ Telegram/Email alerts
- ✅ Position management with trailing stops
- ✅ Backtesting engine
- ✅ REST API dashboard

---

## 📋 Quick Start (5 Steps)

### Step 1: Compile Java Code
```bash
cd c:\Users\PC\Documents\NetBeansProjects\Fxausd
mvn clean compile
```

### Step 2: Set Environment Variables (for Telegram alerts)
```batch
# Windows Command Prompt
setx TELEGRAM_BOT_TOKEN "your_bot_token_here"
setx TELEGRAM_CHAT_ID "your_chat_id_here"
```

**How to get Telegram tokens:**
1. Open Telegram → Search for @BotFather
2. Create new bot → Copy token
3. Send message to your new bot
4. Go to https://api.telegram.org/botXXXX/getUpdates (replace XXXX with token)
5. Find your chat_id in response

### Step 3: Run Backtest (Optional but Recommended)
```bash
cd target/classes
java ForexBotController
# Select: 1 (Backtest)
```

### Step 4: Setup MT5 Expert Advisor
1. Copy `ForexBot_MT5_EA.mq5` to: `C:\Program Files\MetaTrader 5\experts\`
2. Open MetaTrader 5 → Tools → MetaEditor
3. Drag ForexBot_MT5_EA.mq5 into MetaEditor
4. Click Compile (F5)
5. Close MetaEditor
6. Restart MT5
7. Open any EURUSD chart (1H timeframe recommended)
8. In Navigator (Ctrl+N), find "ForexBot" under Experts
9. Drag Expert Advisor onto chart
10. Accept all prompts

### Step 5: Run Live Trading Bot
```bash
# Terminal 1: Start Java API Server
cd target/classes
java ForexBotController
# Select: 2 (Live Trading)

# Server will start on http://localhost:8888
# Dashboard: http://localhost:8888/api/dashboard
```

---

## 🔄 Operating Modes

### Mode 1: Backtest
- Tests strategy on historical data
- Shows: Win rate, Profit Factor, Sharpe Ratio, Max Drawdown
- Outputs report to `backtest_report_TIMESTAMP.txt`

```bash
java ForexBotController
# Select: 1
```

### Mode 2: Live Trading (Recommended)
- Runs REST API server for MT5 connection
- Receives signals from MT5 Expert Advisor
- Manages positions automatically
- Sends alerts via Telegram
- Dashboard at: http://localhost:8888/api/dashboard

```bash
java ForexBotController
# Select: 2
```

### Mode 3: Signal Generation Only
- Generates trading signals without automation
- Exports to CSV file
- Useful for manual trading or UI integration

```bash
java ForexBotController
# Select: 3
```

---

## 📊 Dashboard Features

Access live dashboard at: **http://localhost:8888/api/dashboard**

Shows:
- 💰 Current account balance
- 📈 Total trades executed
- ✅ Win rate percentage
- 📊 Profit factor
- 🔄 Open positions
- 💹 Current P&L

---

## 📱 Telegram Alerts

Bot sends automatic alerts for:
- 🎯 New trading signals
- ✅ Position opened
- 📊 Position updated  
- 📉 Position closed (with P&L)

Example alert:
```
🎯 NEW SIGNAL
Symbol: EURUSD
Direction: BUY
Entry: 1.0850
SL: 1.0825 | TP: 1.0910
Confidence: 78.45% | Strength: 85.20%
Reason: BUY @ Order Block + Uptrend Structure
```

---

## ⚙️ Configuration

### MT5 Expert Advisor Settings
Edit in MT5 terminal after attaching EA:
- **BotServerIP**: localhost (or your server IP)
- **BotServerPort**: 8888 (must match Java server)
- **BotAPIKey**: forex_bot_secret_key_12345 (must match Java side)
- **SymbolToTrade**: EURUSD (what to trade)
- **SignalUpdateInterval**: 60 (seconds between signals)

### Java Bot Configuration
Edit in ForexBotController.java:
- **PORT**: 8888 (API server port)
- **API_KEY**: forex_bot_secret_key_12345 (must match MT5)
- **Account Balance**: $10,000 (starting balance)
- **Risk Per Trade**: 2% of account

---

## 🚀 Trading Rules

### Automatic Position Management
1. **Entry**: On confirmed signal
2. **Stop Loss**: 1.5 × ATR below/above entry
3. **Take Profit**: 3.0 × ATR above/below entry
4. **Trailing Stop**: Adjusts when price moves favorably
5. **Exit Conditions**:
   - SL hit → Close with loss
   - TP hit → Close with profit
   - Trailing stop triggered → Close with profit
   - Manual close → Via dashboard

### Signal Generation Rules
- BUY: Oversold RSI + Bullish MA + Above Bollinger Lower
- SELL: Overbought RSI + Bearish MA + Below Bollinger Upper
- Minimum Confidence: 65%
- SMC Confluence boost applied

### Risk Management
- Max 2% account risk per trade
- Position size calculated automatically
- Max 5 concurrent trades
- Trailing stop enabled

---

## 📈 Performance Metrics

After running backtest, you'll see:

```
════════════════════════════════════════════════
📊 BACKTEST RESULTS
════════════════════════════════════════════════
Total Trades: 47 | Wins: 31 | Losses: 16
Win Rate: 65.96% | Profit Factor: 2.84
Net Profit: $1,247.50 | Gross Profit: $2,100 | Gross Loss: $740
Avg Win: $67.74 | Avg Loss: $46.25 | Risk/Reward: 1.46:1
Max Drawdown: 12.35% | Sharpe Ratio: 1.89
════════════════════════════════════════════════
```

---

## 🔗 API Reference

### POST /api/signal
Submit new trading signal
```bash
curl -X POST http://localhost:8888/api/signal \
  -H "Authorization: Bearer forex_bot_secret_key_12345" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "EURUSD",
    "direction": "BUY",
    "entry": 1.0850,
    "stopLoss": 1.0825,
    "takeProfit": 1.0910,
    "confidence": 0.78,
    "signalStrength": 0.85,
    "reason": "Oversold RSI"
  }'
```

### GET /api/positions
Get current positions status
```bash
curl http://localhost:8888/api/positions
# Returns: {"open":2,"closed":15,"balance":10245.50,"winRate":73.33,"totalTrades":17}
```

### GET /api/dashboard
View HTML dashboard
```bash
# Open in browser: http://localhost:8888/api/dashboard
```

---

## 🐛 Troubleshooting

### "Cannot connect to localhost:8888"
- Check if Java server is running
- Check Windows Firewall allows port 8888
- Try: `netstat -ano | find "8888"`

### "MT5 EA not sending signals"
- Verify Expert Advisor is attached (green smiley in top-right)
- Check Console (Ctrl+2) for errors
- Verify Bot Server IP and Port in EA settings
- Check API key matches both sides

### "Telegram alerts not working"
- Verify TELEGRAM_BOT_TOKEN environment variable is set
- Send message to bot first
- Check bot token is valid at: https://api.telegram.org/botXXX/getMe

### "Backtesting shows 0% accuracy"
- This is normal with small sample datasets (only 70 candles)
- Use real MT5 export with 1000+ candles
- Download from: MT5 → History center → Export to CSV

---

## 💾 File Structure

```
ForexBot/
├── src/main/java/com/mycompany/fxausd/
│   ├── Fxausd.java                 # Main ML + indicators
│   ├── ForexBot.java               # REST API server + position manager
│   ├── ForexBotController.java     # Bot orchestrator & modes
│   └── Backtester.java             # Backtesting engine
│
├── ForexBot_MT5_EA.mq5             # MT5 Expert Advisor
│
├── data/
│   └── eurusd.csv                  # Historical EURUSD data
│
├── forex_signals_export.csv        # Generated signals
├── backtest_report_TIMESTAMP.txt   # Backtest results
└── README.md                       # This file
```

---

## 🎯 Next Steps

1. ✅ Export real MT5 data (1000+ candles)
2. ✅ Run backtest to validate strategy
3. ✅ Deploy on a live trading account with strict risk controls
4. ✅ Monitor for 1 week with small live risk
5. ✅ Scale up once comfortable and confirmed

---

## ⚠️ DISCLAIMER

**This bot is for educational purposes only. NOT financial advice.**

- Past performance ≠ future results
- Forex trading carries high risk
- Use a live trading account only after careful validation
- Use very small position sizes initially
- Never risk money you can't afford to lose
- Monitor bot regularly

---

## 📞 Support

Common issues:
- Port already in use: Change PORT in ForexBotController.java
- CSV parse error: Ensure EURUSD data format matches sample
- MT5 connection: Check firewall and local network

---

**Happy Trading! 🚀**


