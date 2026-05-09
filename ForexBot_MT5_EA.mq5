//+------------------------------------------------------------------+
//| ForexBot MT5 Expert Advisor                                      |
//| Sends trading signals to Java ForexBot API Server                |
//+------------------------------------------------------------------+

#property strict

// Bot Configuration
input string BotServerIP = "localhost";
input int BotServerPort = 8888;
input string BotAPIKey = "forex_bot_secret_key_12345";
input string SymbolToTrade = "EURUSD";
input int SignalUpdateInterval = 60; // seconds

// Indicator parameters
input int RSIPeriod = 14;
input int MA20Period = 20;
input int MA50Period = 50;
input int ATRPeriod = 14;
input int BollingerPeriod = 20;

// Global variables
datetime lastSignalTime = 0;
double lastSignalEntry = 0.0;

//+------------------------------------------------------------------+
//| Expert initialization                                             |
//+------------------------------------------------------------------+
int OnInit()
{
    Print("ForexBot MT5 Advisor initialized for " + SymbolToTrade);
    return(INIT_SUCCEEDED);
}

//+------------------------------------------------------------------+
//| Expert tick function                                               |
//+------------------------------------------------------------------+
void OnTick()
{
    if(TimeCurrent() - lastSignalTime < SignalUpdateInterval)
        return;

    int rsiHandle = iRSI(SymbolToTrade, PERIOD_H1, RSIPeriod, PRICE_CLOSE);
    int ma20Handle = iMA(SymbolToTrade, PERIOD_H1, MA20Period, 0, MODE_SMA, PRICE_CLOSE);
    int ma50Handle = iMA(SymbolToTrade, PERIOD_H1, MA50Period, 0, MODE_SMA, PRICE_CLOSE);
    int atrHandle = iATR(SymbolToTrade, PERIOD_H1, ATRPeriod);
    int bbHandle = iBands(SymbolToTrade, PERIOD_H1, BollingerPeriod, 2, 0, PRICE_CLOSE);

    if(rsiHandle == INVALID_HANDLE || ma20Handle == INVALID_HANDLE || ma50Handle == INVALID_HANDLE || atrHandle == INVALID_HANDLE || bbHandle == INVALID_HANDLE)
    {
        Print("❌ Failed to create indicator handles for MT5.");
        return;
    }

    double rsiBuffer[1];
    double ma20Buffer[1];
    double ma50Buffer[1];
    double atrBuffer[1];
    double bbUpperBuffer[1];
    double bbLowerBuffer[1];

    if(CopyBuffer(rsiHandle, 0, 0, 1, rsiBuffer) <= 0 ||
       CopyBuffer(ma20Handle, 0, 0, 1, ma20Buffer) <= 0 ||
       CopyBuffer(ma50Handle, 0, 0, 1, ma50Buffer) <= 0 ||
       CopyBuffer(atrHandle, 0, 0, 1, atrBuffer) <= 0 ||
       CopyBuffer(bbHandle, 0, 0, 1, bbUpperBuffer) <= 0 ||
       CopyBuffer(bbHandle, 2, 0, 1, bbLowerBuffer) <= 0)
    {
        Print("❌ Failed to retrieve MT5 indicator values.");
        return;
    }

    double rsi = rsiBuffer[0];
    double ma20 = ma20Buffer[0];
    double ma50 = ma50Buffer[0];
    double atr = atrBuffer[0];
    double bbUpper = bbUpperBuffer[0];
    double bbLower = bbLowerBuffer[0];
    double closePrice = SymbolInfoDouble(SymbolToTrade, SYMBOL_BID);
    double askPrice = SymbolInfoDouble(SymbolToTrade, SYMBOL_ASK);

    string direction = "NONE";
    string reason = "";
    double confidence = 0.0;

    if(rsi < 30 && ma20 > ma50 && closePrice > bbLower)
    {
        direction = "BUY";
        reason = "Oversold RSI + Bullish MA Cross + Above Lower BB";
        confidence = 0.75;
    }
    else if(rsi > 70 && ma20 < ma50 && closePrice < bbUpper)
    {
        direction = "SELL";
        reason = "Overbought RSI + Bearish MA Cross + Below Upper BB";
        confidence = 0.72;
    }

    if(direction != "NONE")
    {
        double entry = askPrice;
        double stopLoss, takeProfit;

        if(direction == "BUY")
        {
            stopLoss = entry - 1.5 * atr;
            takeProfit = entry + 3.0 * atr;
        }
        else
        {
            stopLoss = entry + 1.5 * atr;
            takeProfit = entry - 3.0 * atr;
        }

        SendSignalToBot(direction, entry, stopLoss, takeProfit, confidence, reason);
        lastSignalTime = TimeCurrent();
        lastSignalEntry = entry;
    }

    IndicatorRelease(rsiHandle);
    IndicatorRelease(ma20Handle);
    IndicatorRelease(ma50Handle);
    IndicatorRelease(atrHandle);
    IndicatorRelease(bbHandle);
}

//+------------------------------------------------------------------+
//| Send signal to ForexBot Java API                                  |
//+------------------------------------------------------------------+
void SendSignalToBot(string direction, double entry, double sl, double tp, double confidence, string reason)
{
    string json = "{\"symbol\":\"" + SymbolToTrade + "\"," +
                  "\"direction\":\"" + direction + "\"," +
                  "\"entry\":" + DoubleToString(entry, 5) + "," +
                  "\"stopLoss\":" + DoubleToString(sl, 5) + "," +
                  "\"takeProfit\":" + DoubleToString(tp, 5) + "," +
                  "\"confidence\":" + DoubleToString(confidence, 2) + "," +
                  "\"signalStrength\":0.85," +
                  "\"reason\":\"" + reason + "\"}";

    char postData[];
    StringToCharArray(json, postData, 0, WHOLE_ARRAY);

    string headers = "Authorization: Bearer " + BotAPIKey + "\r\n" +
                     "Content-Type: application/json\r\n";
    string url = "http://" + BotServerIP + ":" + IntegerToString(BotServerPort) + "/api/signal";
    char result[];
    string resultHeaders;
    int timeout = 10000;

    int responseCode = WebRequest("POST", url, headers, timeout, postData, result, resultHeaders);

    if(responseCode == 200)
    {
        Print("✅ Signal sent to ForexBot: " + direction + " @ " + DoubleToString(entry, 5));
    }
    else
    {
        Print("❌ Failed to send signal. Response code: " + IntegerToString(responseCode));
    }
}

//+------------------------------------------------------------------+
//| Expert deinitialization                                           |
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
{
    Print("ForexBot MT5 Advisor deinitialized");
}
