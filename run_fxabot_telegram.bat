@echo off
REM Run Fxausd live mode with Telegram notification environment variables.
REM Usage: run_fxabot_telegram.bat YOUR_TELEGRAM_BOT_TOKEN YOUR_CHAT_ID

if "%1"=="" (
  echo Usage: %~nx0 BOT_TOKEN CHAT_ID
  exit /b 1
)
if "%2"=="" (
  echo Usage: %~nx0 BOT_TOKEN CHAT_ID
  exit /b 1
)

set TELEGRAM_BOT_TOKEN=%1
set TELEGRAM_CHAT_ID=%2

if not defined FOREXBOT_PORT (
  if "%3"=="" (
    set FOREXBOT_PORT=8888
  ) else (
    set FOREXBOT_PORT=%3
  )
)

cd /d "%~dp0"

echo Building classpath...
call mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
if errorlevel 1 (
  echo Maven classpath build failed.
  exit /b 1
)

set /p CP=<cp.txt

echo Starting Fxausd live with Telegram notifications on port %FOREXBOT_PORT%...
java -cp "target/classes;%CP%" com.mycompany.fxausd.Fxausd live
