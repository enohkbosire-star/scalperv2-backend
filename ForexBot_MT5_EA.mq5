//+------------------------------------------------------------------+
//| ForexBot MT5 Expert Advisor - Quantum QILH                      |
//+------------------------------------------------------------------+
#property strict

// Bot Configuration
input string BotServerIP = "192.168.100.37"; // YOUR LOCAL IP
input int BotServerPort = 4567;
input string BotAPIKey = "forex_bot_secret_key_12345";
input string SymbolToTrade = ""; // Blank for current chart
input int SignalUpdateInterval = 60; 

// Quantum Parameters
input ENUM_TIMEFRAMES TrendTimeframe = PERIOD_H4;
input ENUM_TIMEFRAMES TriggerTimeframe = PERIOD_H1;

// Global variables
datetime lastSignalTime = 0;
string ActiveSymbol = "";

int OnInit() {
    ActiveSymbol = (SymbolToTrade == "") ? _Symbol : SymbolToTrade;
    Print("🚀 Quantum QILH initialized for " + ActiveSymbol);
    return(INIT_SUCCEEDED);
}

void OnTick() {
    if(TimeCurrent() - lastSignalTime < SignalUpdateInterval) return;

    // 1. Quantum Trend (H4)
    double h4_ema200 = iMA(ActiveSymbol, TrendTimeframe, 200, 0, MODE_EMA, PRICE_CLOSE);
    double h4_close = iClose(ActiveSymbol, TrendTimeframe, 0);
    bool isUptrend = (h4_close > h4_ema200);
    bool isDowntrend = (h4_close < h4_ema200);

    // 2. Trigger (H1)
    double h1_rsi = iRSI(ActiveSymbol, TriggerTimeframe, 14, PRICE_CLOSE);
    
    string direction = "NONE";
    if(isUptrend && h1_rsi < 35) direction = "BUY";
    if(isDowntrend && h1_rsi > 65) direction = "SELL";

    if(direction != "NONE") {
        double entry = (direction == "BUY") ? SymbolInfoDouble(ActiveSymbol, SYMBOL_ASK) : SymbolInfoDouble(ActiveSymbol, SYMBOL_BID);
        
        // Correct ATR Calculation for MT5
        int atrHandle = iATR(ActiveSymbol, TriggerTimeframe, 14);
        double atrBuffer[1];
        CopyBuffer(atrHandle, 0, 0, 1, atrBuffer);
        double atrValue = atrBuffer[0];
        IndicatorRelease(atrHandle);

        double sl = (direction == "BUY") ? entry - 2.0 * atrValue : entry + 2.0 * atrValue;
        double tp = (direction == "BUY") ? entry + 4.0 * atrValue : entry - 4.0 * atrValue;

        SendSignalToBot(direction, entry, sl, tp, 0.90, "Quantum QILH Setup");
        lastSignalTime = TimeCurrent();
    }
}

void SendSignalToBot(string direction, double entry, double sl, double tp, double confidence, string reason) {
    string json = "{\"pair\":\"" + ActiveSymbol + "\",\"action\":\"" + direction + "\",\"entry\":" + DoubleToString(entry, 5) + 
                  ",\"sl\":" + DoubleToString(sl, 5) + ",\"tp\":" + DoubleToString(tp, 5) + ",\"confidence\":" + DoubleToString(confidence, 2) + 
                  ",\"strength\":0.90,\"reason\":\"" + reason + "\"}";

    char postData[];
    StringToCharArray(json, postData, 0, WHOLE_ARRAY);
    string headers = "Authorization: Bearer " + BotAPIKey + "\r\nContent-Type: application/json\r\n";
    string url = "http://" + BotServerIP + ":" + (string)BotServerPort + "/signals/add"; 
    char result[];
    string resultHeaders;

    WebRequest("POST", url, headers, 10000, postData, result, resultHeaders);
    Print("✅ Signal Sent: " + direction + " " + ActiveSymbol);
}                   