import MetaTrader5 as mt5
from flask import Flask, request, jsonify
import logging

# Configure Logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)

# 1. Initialize MetaTrader 5
if not mt5.initialize():
    logger.error("❌ MetaTrader5 initialization failed. Check if MT5 is installed and logged in.")
    quit()

logger.info("✅ MetaTrader 5 Connected Successfully")
logger.info("🚀 Execution Bridge is LIVE on port 5005")

# --- NEW: CANDLE FETCHING ENDPOINT ---
@app.route('/api/candles', methods=['GET'])
@app.route('/candles', methods=['GET'])
def get_candles():
    try:
        symbol = request.args.get('symbol', 'XAUUSD')
        timeframe_str = request.args.get('timeframe', 'M5')
        count = int(request.args.get('count', 220))

        # Map MT5 Timeframes
        tf_map = {
            'M1': mt5.TIMEFRAME_M1, 'M5': mt5.TIMEFRAME_M5, 'M15': mt5.TIMEFRAME_M15,
            'M30': mt5.TIMEFRAME_M30, 'H1': mt5.TIMEFRAME_H1, 'H4': mt5.TIMEFRAME_H4,
            'D1': mt5.TIMEFRAME_D1, 'W1': mt5.TIMEFRAME_W1, 'MN1': mt5.TIMEFRAME_MN1
        }
        mt5_tf = tf_map.get(timeframe_str.upper(), mt5.TIMEFRAME_M5)

        # Fetch rates from MT5
        rates = mt5.copy_rates_from_pos(symbol, mt5_tf, 0, count)
        if rates is None:
            return jsonify({"status": "error", "message": f"Failed to fetch rates for {symbol}"}), 404

        # Convert to list of dicts for Java bot
        candles = []
        for r in rates:
            candles.append({
                "time": int(r['time']),
                "open": float(r['open']),
                "high": float(r['high']),
                "low": float(r['low']),
                "close": float(r['close']),
                "volume": float(r['tick_volume'])
            })

        return jsonify(candles)
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

# --- NEW: SYMBOLS ENDPOINT ---
@app.route('/api/symbols', methods=['GET'])
@app.route('/symbols', methods=['GET'])
def get_symbols():
    try:
        symbols = mt5.symbols_get()
        if symbols is None:
            return jsonify([])
        return jsonify([s.name for s in symbols if s.visible])
    except Exception as e:
        return jsonify([])

@app.route('/api/order', methods=['POST'])
def handle_order():
    try:
        data = request.json
        logger.info(f"📥 Received Trade: {data['symbol']} {data['direction']}")

        symbol = data['symbol']
        direction = data['direction']
        entry = float(data.get('entry', 0))
        sl = float(data.get('stopLoss', 0))
        tp = float(data.get('takeProfit', 0))

        # Check if symbol exists in MT5
        symbol_info = mt5.symbol_info(symbol)
        if symbol_info is None:
            logger.error(f"❌ Symbol {symbol} not found in MT5")
            return jsonify({"status": "error", "message": f"Symbol {symbol} not found"}), 404

        # Prepare trade request
        order_type = mt5.ORDER_TYPE_BUY if direction == "BUY" else mt5.ORDER_TYPE_SELL
        price = mt5.symbol_info_tick(symbol).ask if direction == "BUY" else mt5.symbol_info_tick(symbol).bid

        request_data = {
            "action": mt5.TRADE_ACTION_DEAL,
            "symbol": symbol,
            "volume": 0.01,  # Fixed lot size for safety, you can make this dynamic
            "type": order_type,
            "price": price,
            "sl": sl,
            "tp": tp,
            "magic": 234567,
            "comment": "QFE Java Fusion",
            "type_time": mt5.ORDER_TIME_GTC,
            "type_filling": mt5.ORDER_FILLING_IOC,
        }

        # Send trade to MT5
        result = mt5.order_send(request_data)

        if result.retcode != mt5.TRADE_RETCODE_DONE:
            logger.error(f"❌ Trade Failed: {result.comment}")
            return jsonify({"status": "error", "message": result.comment}), 400

        logger.info(f"✅ Trade Executed: Ticket #{result.order}")
        return jsonify({"status": "success", "ticket": result.order}), 200

    except Exception as e:
        logger.error(f"💥 Critical Error: {str(e)}")
        return jsonify({"status": "error", "message": str(e)}), 500

if __name__ == '__main__':
    # Changed host to 0.0.0.0 to support ngrok tunneling
    app.run(host='0.0.0.0', port=5005)
