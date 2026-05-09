import urllib.request
import urllib.error
import json

symbols = ['EURUSD', 'GBPUSD', 'USDJPY', 'AUDUSD', 'USDCAD', 'NZDUSD']
for symbol in symbols:
    for tf in ['M15', 'H1']:
        url = f'http://127.0.0.1:5000/api/candles?symbol={symbol}&count=220&timeframe={tf}'
        try:
            with urllib.request.urlopen(url, timeout=30) as resp:
                data = json.loads(resp.read().decode('utf-8', errors='replace'))
                print(symbol, tf, 'count', len(data))
        except urllib.error.HTTPError as e:
            body = e.read().decode('utf-8', errors='replace')
            print(symbol, tf, 'HTTP', e.code, body[:200])
        except Exception as e:
            print(symbol, tf, 'ERR', type(e).__name__, e)
