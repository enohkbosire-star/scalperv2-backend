@echo off
REM ForexBot Quick Start Script for Windows
REM This script compiles and runs the ForexBot

SETLOCAL ENABLEDELAYEDEXPANSION

:menu
CLS
ECHO.
ECHO ╔══════════════════════════════════════════════════════════╗
ECHO ║          ForexBot - Quick Start for Windows             ║
ECHO ║                Version 1.0                              ║
ECHO ╚══════════════════════════════════════════════════════════╝
ECHO.
ECHO ╔─ SELECT OPTION ────────────────────────────────────────╗
ECHO ║                                                          ║
ECHO ║  1. 🔧 Setup (First time only)                         ║
ECHO ║  2. 🔨 Compile Project                                 ║
ECHO ║  3. 🔬 Run Backtest                                    ║
ECHO ║  4. 🤖 Run Live Bot (MT5 Integration)                  ║
ECHO ║  5. 📊 Generate Trading Signals Only                   ║
ECHO ║  6. ⚙️  View Configuration Guide                        ║
ECHO ║  7. 📖 Open Setup Guide (README)                       ║
ECHO ║  0. ❌ Exit                                             ║
ECHO ║                                                          ║
ECHO ╚──────────────────────────────────────────────────────────╝
ECHO.

SET /P choice="Enter your choice (0-7): "

IF "%choice%"=="0" GOTO end
IF "%choice%"=="1" GOTO setup
IF "%choice%"=="2" GOTO compile
IF "%choice%"=="3" GOTO backtest
IF "%choice%"=="4" GOTO live
IF "%choice%"=="5" GOTO signals
IF "%choice%"=="6" GOTO config
IF "%choice%"=="7" GOTO readme

ECHO Invalid choice. Please try again.
TIMEOUT /T 2 /NOBREAK
GOTO menu

:setup
CLS
ECHO.
ECHO 🔧 ForexBot Setup
ECHO ════════════════════════════════════════════════════════════
ECHO.
ECHO Step 1: Setting Telegram environment variables
ECHO.
SET /P telegram_token="Enter your Telegram Bot Token (from @BotFather): "
SET /P telegram_chat="Enter your Telegram Chat ID: "

ECHO.
ECHO Setting environment variables...
setx TELEGRAM_BOT_TOKEN "%telegram_token%" >NUL 2>&1
setx TELEGRAM_CHAT_ID "%telegram_chat%" >NUL 2>&1

ECHO.
ECHO ✅ Environment variables set!
ECHO.
ECHO Step 2: Installing Maven dependencies...
CALL mvn clean install -q

ECHO.
ECHO ✅ Setup complete!
ECHO.
ECHO 📋 Next Steps:
ECHO   1. Copy ForexBot_MT5_EA.mq5 to your MT5 experts folder
ECHO   2. Run option 2 to compile
ECHO   3. Run option 4 to start the bot
ECHO.
PAUSE
GOTO menu

:compile
CLS
ECHO.
ECHO 🔨 Compiling ForexBot...
ECHO ════════════════════════════════════════════════════════════
ECHO.
CALL mvn clean compile -q

IF %ERRORLEVEL% EQU 0 (
    ECHO ✅ Compilation successful!
) ELSE (
    ECHO ❌ Compilation failed. Check errors above.
)

ECHO.
PAUSE
GOTO menu

:backtest
CLS
ECHO.
ECHO 🔬 Running Backtest...
ECHO ════════════════════════════════════════════════════════════
ECHO.
CD target\classes
JAVA -cp . ForexBotController
CHOICE /C:12 /M "1=Back to Menu, 2=Compile and Back: "
IF ERRORLEVEL 2 GOTO compile
IF ERRORLEVEL 1 GOTO menu

:live
CLS
ECHO.
ECHO 🤖 Starting Live Bot (MT5 Integration)
ECHO ════════════════════════════════════════════════════════════
ECHO.
ECHO ⚠️  Make sure:
ECHO   • MT5 Expert Advisor is attached to your chart
ECHO   • Telegram environment variables are set (Option 1)
ECHO   • Port 8888 is available
ECHO.
ECHO 📊 Dashboard will be available at:
ECHO    http://localhost:8888/api/dashboard
ECHO.
ECHO Starting Java API Server...
ECHO.
CD target\classes
JAVA -cp . ForexBotController
PAUSE
GOTO menu

:signals
CLS
ECHO.
ECHO 📊 Generating Trading Signals...
ECHO ════════════════════════════════════════════════════════════
ECHO.
CD target\classes
JAVA -cp . ForexBotController
ECHO.
PAUSE
GOTO menu

:config
CLS
ECHO.
ECHO ⚙️  Configuration Guide
ECHO ════════════════════════════════════════════════════════════
ECHO.
ECHO 1. TELEGRAM SETUP:
ECHO    • Search @BotFather on Telegram
ECHO    • Create new bot and copy token
ECHO    • Send /start to your bot
ECHO    • Get chat ID from: https://api.telegram.org/botXXXX/getUpdates
ECHO.
ECHO 2. MT5 EXPERT ADVISOR SETUP:
ECHO    • Copy ForexBot_MT5_EA.mq5 to: C:\Program Files\MetaTrader 5\experts\
ECHO    • Open MetaTrader → Tools → MetaEditor
ECHO    • Compile the EA (F5)
ECHO    • Attach to EURUSD 1H chart
ECHO.
ECHO 3. JAVA BOT SERVER:
ECHO    • Run "4. Run Live Bot" from this menu
ECHO    • Server starts on http://localhost:8888
ECHO.
ECHO 4. VERIFY CONNECTION:
ECHO    • Dashboard: http://localhost:8888/api/dashboard
ECHO    • Telegram: Check for bot alerts
ECHO.
PAUSE
GOTO menu

:readme
START "" "FOREXBOT_SETUP_GUIDE.md"
GOTO menu

:end
CLS
ECHO.
ECHO Thanks for using ForexBot! 
ECHO ✅ Happy Trading!
ECHO.
TIMEOUT /T 2 /NOBREAK
EXIT /B 0


