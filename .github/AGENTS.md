# Fxausd Workspace Agent Guidance

This file helps AI coding agents work productively in the Fxausd Java Forex trading bot repository.

## What this project is

- Java/Maven Forex trading bot with machine learning, technical indicators, MT5 integration, and a backtesting engine.
- Core source path: `src/main/java/com/mycompany/fxausd/`
- Primary Java classes:
  - `Fxausd.java` — ML, indicators, trading signal generation, classifiers, and live strategy logic
  - `ForexBot.java` — REST API server, position manager, news filter, and notification logic
  - `ForexBotController.java` — mode selection, backtest/live/signal orchestration
  - `Backtester.java` — historical backtest engine

## Build and run

- Build with Maven:
  - `mvn clean compile`
  - `mvn package`
- Run the main class from the project root:
  - `java -cp target/classes com.mycompany.fxausd.Fxausd`
- The project targets Java 21 and packages a shaded JAR via the Maven Shade plugin.

## Important repository conventions

- Do not edit compiled `.class` files in `src/main/java/com/mycompany/fxausd/`; they are generated artifacts in the source tree.
- Prefer edits to `.java` source files only.
- This repo contains support scripts and documentation for Windows (`forexbot_quickstart.bat`, `README_FOREXBOT.md`, `FOREXBOT_SETUP_GUIDE.md`).
- `ForexBot_MT5_EA.mq5` is MetaTrader side integration; modify it only when the change clearly involves the MT5 Expert Advisor interaction.

## Useful docs

- `README_FOREXBOT.md` — project overview, setup, and operating modes
- `FOREXBOT_SETUP_GUIDE.md` — setup instructions and environment configuration

## Best use cases for AI agents

- Fix Java logic in the trading bot, backtester, live trading flow, or REST API behavior.
- Update Maven configuration, dependency settings, or packaging behavior.
- Improve reliability for MT5 signal handling, Telegram alerts, position sizing, and risk management.
- Clarify or improve documentation in the project README files.

## Avoid

- Changing log files, build output files, or unrelated generated artifacts in the repo root.
- Adding broad new frameworks or architecture that does not fit the existing Java/Maven Forex bot structure.
- Editing compiled class files or binary assets unless explicitly requested.
