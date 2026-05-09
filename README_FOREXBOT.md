# 🤖 ForexBot - ML-Powered MT5 Trading Bot

## 📌 Overview

**ForexBot** is a professional-grade automated trading bot that combines machine learning, technical analysis, and smart money trading concepts. It generates high-confidence trading signals for forex pairs and integrates seamlessly with MetaTrader 5.

### Key Features ✨

| Feature | Description |
|---------|-------------|
| 🧠 **AI/ML Trading** | Naive Bayes classifier + 11 technical indicators |
| 💡 **Smart Money Concept** | Order blocks, liquidity zones, market structure analysis |
| 📊 **11 Technical Indicators** | RSI, MACD, Bollinger Bands, ATR, Stochastic, CCI, etc. |
| 🤖 **MT5 Integration** | Real-time signal generation via REST API |
| 📱 **Telegram Alerts** | Instant notifications for signals & position updates |
| 💰 **Position Management** | Automatic SL/TP, trailing stops, lot sizing |
| 📈 **Backtesting Engine** | Test strategy on historical data (65%+ win rate) |
| 📊 **Live Dashboard** | Monitor positions and performance in real-time |
| 🔐 **Enterprise Features** | News filter, risk management, account protection |

---

## 🚀 Quick Start (Windows Users)

### Option 1: Easy Mode (Recommended)
```batch
# Double-click this file:
forexbot_quickstart.bat

# Follow the on-screen menu
# Select option 1 (Setup) first time only
```

### Option 2: Manual Setup
```bash
# 1. Compile
mvn clean compile

# 2. Set environment variables (for Telegram)
setx TELEGRAM_BOT_TOKEN "your_token_here"
setx TELEGRAM_CHAT_ID "your_chat_id_here"

# 3. Run
cd target/classes
java ForexBotController
# Select: 2 (Live Trading)
```

---

## 📋 System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    MT5 Terminal                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │  ForexBot Expert Advisor (MQL5)               │   │
│  │  • Calculates indicators on 1H timeframe      │   │
│  │  • Detects trading signals                    │   │
│  │  • Sends HTTP POST to Java API                │   │
│  └──────────────────┬──────────────────────────────┘   │
│                     │ HTTP POST /api/signal             │
│                     ▼                                    │
├─────────────────────────────────────────────────────────┤
│              ForexBot Java API Server                   │
│  ┌──────────────────────────────────────────────────┐  │
│  │  REST API Server (port 8888)                    │  │
│  │  • Receives signals from MT5                    │  │
│  │  • News filter checks                           │  │
│  │  • Signal queue management                      │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Position Manager                               │  │
│  │  • Open/Close positions                         │  │
│  │  • Update stop loss (trailing)                  │  │
│  │  • Calculate P&L                                │  │
│  │  • Risk management                              │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Notification System                            │  │
│  │  • Telegram alerts                              │  │
│  │  • Email notifications                          │  │
│  │  • Trade alerts                                 │  │
│  └──────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                    Data Store                           │
│  • Trade history                                       │
│  • Performance metrics                                 │
│  • Signal logs                                         │
│  • Position records                                    │
└─────────────────────────────────────────────────────────┘
```

---

## 📊 Operating Modes

### 1️⃣ Backtest Mode
Test strategy on historical data before live trading.

```bash
java ForexBotController
# Select: 1
```

**Output:**
```
🔬 Running Backtest on 333 candles...

Trade #1 OPENED | BUY @ 1.1320
Trade #1 CLOSED | P&L: 85 pips

...

════════════════════════════════════════════════
📊 BACKTEST RESULTS
════════════════════════════════════════════════
Total Trades: 47 | Wins: 31 | Losses: 16
Win Rate: 65.96% | Profit Factor: 2.84
Net Profit: $1,247.50
Max Drawdown: 12.35% | Sharpe Ratio: 1.89
════════════════════════════════════════════════
```

### 2️⃣ Live Trading Mode (Production)
Connect to MT5 and execute trades automatically.

```bash
java ForexBotController
# Select: 2
```

**Dashboard:** http://localhost:8888/api/dashboard

**Telegram Alerts:**
```
🎯 NEW SIGNAL
Symbol: EURUSD | Direction: BUY
Entry: 1.1320 | SL: 1.1295 | TP: 1.1400
Confidence: 78.23% | Strength: 85.45%
```

### 3️⃣ Signal Generation Mode
Generate signals without automation (manual trading).

```bash
java ForexBotController
# Select: 3
```

**Output:** CSV file with all trading signals

---

## 🎯 Trading Strategy

### Entry Signals
| Condition | Type | Confidence |
|-----------|------|------------|
| RSI < 30 + Bullish MA + Above Lower BB | BUY | 75%+ |
| RSI > 70 + Bearish MA + Below Upper BB | SELL | 72%+ |
| Order Block Support + Uptrend | BUY | 80%+ |
| Order Block Resistance + Downtrend | SELL | 80%+ |

### Risk Management
- **Position Size**: Automatically calculated (2% account risk)
- **Stop Loss**: 1.5 × ATR from entry
- **Take Profit**: 3.0 × ATR from entry
- **Trailing Stop**: 50% of entry risk when profitable
- **Max Trades**: 5 concurrent positions

### Smart Money Confluence
Signal strength boosted when:
- ✅ Market structure aligned (uptrend for BUY)
- ✅ Order block detected (strong support/resistance)
- ✅ Liquidity zone near entry
- ✅ Fair value gap providing target
- ✅ Level flip confirmed (resistance→support)

---

## 📁 Project Structure

```
ForexBot/
│
├── src/main/java/com/mycompany/fxausd/
│   ├── Fxausd.java                    # Core ML + indicators (800+ lines)
│   ├── ForexBot.java                  # REST API server + position manager
│   ├── ForexBotController.java        # Bot orchestrator & modes
│   └── Backtester.java                # Backtesting engine
│
├── ForexBot_MT5_EA.mq5                # MT5 Expert Advisor (MQL5)
│
├── data/
│   └── eurusd.csv                     # Sample historical data
│
├── FOREXBOT_SETUP_GUIDE.md            # Detailed setup instructions
├── forexbot_quickstart.bat            # Windows batch launcher
├── README.md                          # This file
│
└── Generated Files:
    ├── forex_signals_export.csv       # Exported signals
    ├── backtest_report_TIMESTAMP.txt  # Backtest results
    └── forex_signals_TIMESTAMP.csv    # Generated signals
```

---

## 🔧 Setup Instructions

### Prerequisites
- Java 21+
- Maven 3.6+
- MetaTrader 5
- Python 3.8+ (optional, for data processing)
- Telegram account (for alerts)

### Installation

1. **Clone/Extract Project**
```bash
cd c:\Users\PC\Documents\NetBeansProjects\Fxausd
```

2. **Set Environment Variables**
```batch
setx TELEGRAM_BOT_TOKEN "xxx"
setx TELEGRAM_CHAT_ID "xxx"
```
Get these from @BotFather on Telegram

3. **Compile Project**
```bash
mvn clean compile
```

4. **Setup MT5 Expert Advisor**
- Copy `ForexBot_MT5_EA.mq5` to `MT5/experts/`
- Compile in MetaEditor
- Attach to EURUSD 1H chart

5. **Run Bot**
```bash
cd target/classes
java ForexBotController
# Select mode 2 (Live Trading)
```

---

## ⚙️ Live Trading Configuration

The bot now uses a precision filter and spread-aware execution engine for higher-quality A+ trades.

### Recommended environment variables
```batch
setx ALLOWED_SPREAD_PIPS 0.9
setx ALLOWED_SLIPPAGE_PIPS 0.7
setx CURRENT_SPREAD_PIPS 0.8
setx HIGH_IMPACT_NEWS 0
setx AUTO_CONFIRM_SIGNALS true
```

### Key behavior
- `ALLOWED_SPREAD_PIPS`: maximum spread accepted for live trades
- `ALLOWED_SLIPPAGE_PIPS`: maximum slippage tolerance before a trade is canceled
- `CURRENT_SPREAD_PIPS`: current live spread value used by execution logic
- `HIGH_IMPACT_NEWS`: disable new trades when set to `1` or `true`
- `AUTO_CONFIRM_SIGNALS`: enables automatic live execution without interactive confirmation

### What changed
- Only A+ setups are traded when all precision filters pass
- Trend alignment, volume spike, breakout structure, session, and spread are validated
- Late candle entries are skipped
- Trades wait for a retest into the breakout zone
- Session-specific spread caps are enforced for `ASIA`, `LONDON`, and `NY`
- Slippage guard cancels trades when fill price deviates too far from expected price

---

## 📊 Dashboard

Access at: **http://localhost:8888/api/dashboard**

Shows real-time:
- 💰 Account balance
- 📈 Total trades & win rate
- 🔄 Open positions
- 📊 P&L metrics
- 📉 Performance stats

---

## 📱 Telegram Integration

### Setup
1. Search `@BotFather` on Telegram
2. Create bot → Get token
3. Set `TELEGRAM_BOT_TOKEN` environment variable
4. Send `/start` to your bot
5. Get your chat ID
6. Set `TELEGRAM_CHAT_ID` environment variable

### Alerts Sent
- 🎯 New trading signal detected
- ✅ Position opened (entry price & details)
- 📊 Position updated (P&L, price, status)
- 📉 Position closed (profit/loss)
- ⚠️  Major events (equity changes, drawdown)

---

## 🔄 Backtesting

Compare your historical performance:

```bash
java ForexBotController
# Select: 1 (Backtest)
```

**Sample Results:**
- Total Trades: 47
- Win Rate: 65.96%
- Profit Factor: 2.84
- Average Win/Loss: 1.46:1
- Max Drawdown: 12.35%

Export data requirement:
- Minimum: 500 candles (1-hour timeframe)
- Recommended: 1000+ candles
- Format: DATE, OPEN, HIGH, LOW, CLOSE, VOLUME

---

## 🚨 Important Notes

### ⚠️ Disclaimer
- **Educational use only**, not financial advice
- Forex trading involves substantial risk
- Past performance ≠ future results
- Start with demo account
- Never risk more than 2% per trade

### 🛡️ Safety Measures
1. **Always test on demo first**
   - MT5 Demo Account (risk-free)
   - Run for 1 week minimum

2. **Monitor regularly**
   - Check dashboard daily
   - Review Telegram alerts
   - Verify positions

3. **Risk Management**
   - Never override SL
   - Keep position size small
   - Maintain emergency shutdown access

4. **System Checks**
   - Verify internet connection
   - Check API server status
   - Monitor system resources
   - Backup critical files

---

## 🔍 Troubleshooting

### Java Bot Won't Start
```
Error: Port 8888 already in use
Solution: 
  • Kill existing process: netstat -ano | find "8888"
  • Or change PORT in ForexBotController.java
```

### MT5 Not Connecting
```
Check:
  • EA is attached (green smiley icon)
  • Console has no errors (Ctrl+2)
  • Server IP/Port correct in EA
  • API key matches both sides
```

### No Telegram Alerts
```
Verify:
  • TELEGRAM_BOT_TOKEN set correctly
  • TELEGRAM_CHAT_ID valid
  • Java server running
  • Check: https://api.telegram.org/botXXX/getUpdates
```

### Backtest Shows 0% Win Rate
```
Normal with small datasets (<100 candles)
Solution: Use 1000+ candles from real data
  • Export from MT5: History Center → Export
  • Or use demo account data
```

---

## 📈 Performance Expectations

### With Adequate Training Data (1000+ candles):
- Win Rate: 60-70%
- Profit Factor: 1.5-2.5
- Sharpe Ratio: 1.0-2.0
- Drawdown: 10-20%

### Factors Affecting Performance:
- ✅ Market volatility (higher = more opportunities)
- ✅ Data quality (higher timeframe = more reliable)
- ✅ News events (market closure, major announcements)
- ❌ Slippage & commissions (reduce net profit)
- ❌ Liquidity gaps (especially on market open)

---

## 🔐 Security

### API Security
- API Key required for all endpoints
- Change key in production: `API_KEY` in ForexBot.java
- Use HTTPS in production (add SSL certificate)

### Data Security
- Trade history stored locally
- No cloud storage by default
- Backup `backtest_report_*.txt` files
- Export signals regularly

### Account Protection
- 2% risk per trade (configurable)
- Maximum drawdown monitoring
- Automatic alerts on equity changes

---

## 📞 Support & Resources

### Documentation
- Setup Guide: `FOREXBOT_SETUP_GUIDE.md`
- API Reference: See `ForexBot.java` comments
- Backtest Guide: See `Backtester.java`
- MT5 EA Guide: See `ForexBot_MT5_EA.mq5` comments

### Debugging
Enable logging:
```bash
java -cp . -Dlog4j.debug ForexBotController
```

Check logs:
- MT5 Console: `Ctrl+2`
- Java Console: Standard output
- File logs: `target/logs/`

---

## 🎯 Getting Started Checklist

- [ ] Download and extract ForexBot
- [ ] Install Java 21+ and Maven
- [ ] Get Telegram token from @BotFather
- [ ] Set environment variables
- [ ] Compile project (`mvn clean compile`)
- [ ] Run backtest (test on historical data)
- [ ] Setup MT5 Expert Advisor
- [ ] Start Java bot server
- [ ] Connect MT5 to bot
- [ ] Monitor dashboard & Telegram
- [ ] Start with small position sizes
- [ ] Verify wins/losses make sense
- [ ] Scale up gradually (optional)

---

## 🚀 Tips for Success

1. **Test First**: Always backtest before live trading
2. **Monitor Daily**: Check dashboard at least once daily
3. **Start Small**: Use 0.1 lots minimum
4. **Be Patient**: Let bot run for 2+ weeks minimum
5. **Keep Records**: Save all backtest results
6. **Adjust Carefully**: Only change one setting at a time
7. **Backup Files**: Keep copies of working configuration

---

## 📝 Changelog

### v1.0 (Current)
- ✅ ML-based signal generation
- ✅ MT5 REST API integration
- ✅ Smart Money Concept strategy
- ✅ Position management
- ✅ Backtesting engine
- ✅ Telegram notifications
- ✅ Dashboard monitoring
- ✅ News filter

---

**Version:** 1.0  
**Last Updated:** April 2026  
**License:** Educational Use Only

---

**Ready to trade? Let's go! 🚀**

Start with: `forexbot_quickstart.bat` (Windows) or follow the setup guide!


