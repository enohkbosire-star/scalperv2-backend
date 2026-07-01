@echo off
TITLE MT5 Execution Bridge (Python 3.13)
echo 🚀 Restarting MetaTrader 5 Bridge using Python 3.13...
echo.

:: 1. Kill any existing instances of the bridge to ensure a clean start
echo 🔄 Stopping existing bridge processes...
taskkill /F /FI "WINDOWTITLE eq MT5 Execution Bridge*" /T >nul 2>&1

cd /d "C:\Users\PC\Documents\NetBeansProjects\Fxausd"

:: 2. Force the use of Python 3.13 via the launcher
echo 🔍 Checking for Python 3.13...
py -3.13 --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Found Python 3.13. Installing dependencies...
    py -3.13 -m pip install MetaTrader5 flask --quiet
    echo 🚀 Launching Bridge...
    py -3.13 MT5_Execution_Bridge.py
    goto end
)

:: 3. Fallback to standard python if 3.13 launcher fails
echo 🔍 Checking standard 'python' command...
python --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Found 'python'. Installing dependencies...
    python -m pip install MetaTrader5 flask --quiet
    python MT5_Execution_Bridge.py
    goto end
)

echo.
echo ❌ ERROR: Could not start Python 3.13.
echo 💡 Please make sure "python.exe" and "python3.exe" are turned OFF in
echo    Windows "App Execution Aliases" settings.
echo.

:end
pause
