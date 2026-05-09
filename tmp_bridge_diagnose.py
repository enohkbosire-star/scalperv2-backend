import urllib.request
import urllib.error
import json

paths = ['/api/symbols', '/api/candles?symbol=EURUSD&count=10&timeframe=M15']
for path in paths:
    url = 'http://127.0.0.1:5000' + path
    print('REQUEST', url)
    try:
        with urllib.request.urlopen(url, timeout=30) as resp:
            body = resp.read().decode('utf-8', errors='replace')
            print('STATUS', resp.status)
            print(body[:1200])
    except urllib.error.HTTPError as e:
        print('HTTPERR', e.code)
        print(e.read().decode('utf-8', errors='replace')[:1200])
    except Exception as e:
        print('ERR', type(e).__name__, e)
