@echo off
TITLE MT5 Execution Bridge
echo ========================================================
echo 🚀 RESTARTING MT5 EXECUTION BRIDGE
echo ========================================================
echo.

:: 1. Force kill any existing python or bridge processes
echo 🔄 Cleaning up old processes...
taskkill /F /IM python.exe /T >nul 2>&1
taskkill /F /IM py.exe /T >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq MT5 Execution Bridge*" /T >nul 2>&1

:: 2. Navigate to project directory
cd /d "C:\Users\PC\Documents\NetBeansProjects\Fxausd"

:: 3. Find working Python (Checking 3.13 first)
echo 🔍 Searching for Python 3.13...

:: Try 'py' launcher with 3.13
py -3.13 --version >nul 2>&1
if %errorlevel% equ 0 (
    set PY_CMD=py -3.13
    goto start
)

:: Try standard 'python'
python --version >nul 2>&1
if %errorlevel% equ 0 (
    set PY_CMD=python
    goto start
)

:: Try standard 'py'
py --version >nul 2>&1
if %errorlevel% equ 0 (
    set PY_CMD=py
    goto start
)

echo.
echo ❌ ERROR: Python not found!
echo Please ensure Python 3.13 is installed and "Add to PATH" was checked.
pause
exit

:start
echo ✅ Using: %PY_CMD%
echo 📦 Syncing dependencies (MetaTrader5, Flask)...
%PY_CMD% -m pip install MetaTrader5 flask --quiet

echo.
echo ========================================================
echo 🟢 BRIDGE IS LIVE - KEEP THIS WINDOW OPEN
echo ========================================================
echo.
%PY_CMD% MT5_Execution_Bridge.py

echo.
echo ⚠️ Bridge has stopped.
pause
