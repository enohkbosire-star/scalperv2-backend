package com.mycompany.fxausd;

import com.sun.net.httpserver.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.net.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

/**
 * ForexBot - Automated MT5 Trading Bot
 * REST API Server for receiving MT5 signals and managing trades
 */
public class ForexBot {
    
    private static final int DEFAULT_PORT = 8888;
    private static final int PORT = initializePort();
    private static final String API_KEY = "forex_bot_secret_key_12345";
    private static final String DEFAULT_PYTHON_SIGNAL_SERVICE_URL = "http://localhost:6000/api/signals";
    private static final String PYTHON_SIGNAL_SERVICE_URL_ENV = "PYTHON_SIGNAL_SERVICE_URL";
    private static final Gson GSON = new Gson();

    private static int initializePort() {
        // Use FOREXBOT_PORT if available, otherwise fallback to 8888. 
        // Avoid using "PORT" directly here if CloudAPI is also using it to avoid collision on Render.
        String port = System.getenv("FOREXBOT_PORT");
        if (port != null && !port.isEmpty()) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_PORT;
    }

    public static boolean isForexMarketClosed() {
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        DayOfWeek dow = nowUtc.getDayOfWeek();
        int hour = nowUtc.getHour();

        // Forex Market standard: Sunday 21:00 UTC to Friday 21:00 UTC
        if (dow == DayOfWeek.FRIDAY && hour >= 21) return true;
        if (dow == DayOfWeek.SATURDAY) return true;
        if (dow == DayOfWeek.SUNDAY && hour < 21) return true;
        return false;
    }

    public static boolean isForexMarketOpen() {
        return !isForexMarketClosed();
    }
    
    // ===============================
    // BOT SIGNAL QUEUE
    // ===============================
    public static class BotSignal {
        public String symbol;
        public String direction; // BUY/SELL
        public double entry;
        public double stopLoss;
        public double takeProfit;
        public double confidence;
        public double signalStrength;
        public String reason;
        public long timestamp;
        public int ticketNumber;
        public String status; // PENDING, ACTIVE, CLOSED
        
        public BotSignal(String sym, String dir, double ent, double sl, double tp, 
                        double conf, double strength, String reason) {
            symbol = sym;
            direction = dir;
            entry = ent;
            stopLoss = sl;
            takeProfit = tp;
            confidence = conf;
            signalStrength = strength;
            this.reason = reason;
            timestamp = System.currentTimeMillis();
            ticketNumber = (int) timestamp;
            status = "PENDING";
        }
    }
    
    // ===============================
    // POSITION MANAGER
    // ===============================
    public static class Position {
        public int ticket;
        public String symbol;
        public String type; // BUY/SELL
        public double entryPrice;
        public double currentPrice;
        public double stopLoss;
        public double takeProfit;
        public double trailingStopLevel;
        public double lotSize;
        public double pnl;
        public long openTime;
        public long closeTime;
        public String status; // OPEN/CLOSED
        
        public Position(int t, String sym, String tp, double entry, double sl, double tp_price, double lot) {
            ticket = t;
            symbol = sym;
            type = tp;
            entryPrice = entry;
            currentPrice = entry;
            stopLoss = sl;
            takeProfit = tp_price;
            trailingStopLevel = sl;
            lotSize = lot;
            openTime = System.currentTimeMillis();
            status = "OPEN";
        }
        
        public void updatePrice(double newPrice) {
            currentPrice = newPrice;
            pnl = (type.equals("BUY") ? 
                   (newPrice - entryPrice) * 100000 * lotSize :
                   (entryPrice - newPrice) * 100000 * lotSize);
            
            // Update trailing stop
            if (type.equals("BUY") && newPrice > trailingStopLevel) {
                trailingStopLevel = newPrice - Math.abs(entryPrice - stopLoss) * 0.5;
            } else if (type.equals("SELL") && newPrice < trailingStopLevel) {
                trailingStopLevel = newPrice + Math.abs(entryPrice - stopLoss) * 0.5;
            }
        }
        
        public boolean shouldClose(double currentPrice) {
            if (type.equals("BUY")) {
                return currentPrice <= stopLoss || currentPrice >= takeProfit || 
                       currentPrice <= trailingStopLevel;
            } else {
                return currentPrice >= stopLoss || currentPrice <= takeProfit || 
                       currentPrice >= trailingStopLevel;
            }
        }
    }
    
    // ===============================
    // NOTIFICATION SYSTEM
    // ===============================
    public static class Notifier {
        private String telegramBotToken;
        private String telegramChatId;
        private String emailTo;
        
        public Notifier(String token, String chatId, String email) {
            telegramBotToken = token;
            telegramChatId = chatId;
            emailTo = email;
        }
        
        public void sendSignalAlert(BotSignal signal) {
            // Push to Mobile App (Institutional Grade)
            MobileSignalBridge.sendToMobile(
                signal.symbol, 
                signal.direction, 
                signal.entry, 
                signal.takeProfit, 
                signal.stopLoss,
                signal.confidence,
                signal.signalStrength,
                signal.reason,
                0.0, // RiskReward placeholder
                "MT5_API",
                "INSTITUTIONAL"
            );

            String message = String.format(
                "🎯 NEW SIGNAL\n" +
                "Symbol: %s\n" +
                "Direction: %s\n" +
                "Entry: %.5f\n" +
                "SL: %.5f | TP: %.5f\n" +
                "Confidence: %.2f%% | Strength: %.2f%%\n" +
                "Reason: %s\n" +
                "Time: %s",
                signal.symbol, signal.direction, signal.entry, signal.stopLoss, signal.takeProfit,
                signal.confidence * 100, signal.signalStrength, signal.reason,
                formatTime(signal.timestamp)
            );
            
            // Send Telegram
            if (telegramBotToken != null && telegramChatId != null) {
                sendTelegram(message);
            }
            // Send Email (if configured)
            if (emailTo != null) {
                sendEmail("Forex Bot Signal Alert", message);
            }
        }
        
        public void sendPositionUpdate(Position pos) {
            String message = String.format(
                "📊 POSITION UPDATE\n" +
                "Ticket: %d\n" +
                "Symbol: %s | Type: %s\n" +
                "Entry: %.5f | Current: %.5f\n" +
                "P&L: $%.2f | Status: %s\n" +
                "Time: %s",
                pos.ticket, pos.symbol, pos.type, pos.entryPrice, pos.currentPrice,
                pos.pnl, pos.status, formatTime(System.currentTimeMillis())
            );
            
            if (telegramBotToken != null) {
                sendTelegram(message);
            }
        }
        
        private void sendTelegram(String message) {
            try {
                String url = String.format(
                    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                    telegramBotToken, telegramChatId, URLEncoder.encode(message, "UTF-8")
                );
                new URL(url).openConnection().getInputStream().close();
                System.out.println("✅ Telegram alert sent");
            } catch (Exception e) {
                System.out.println("❌ Telegram error: " + e.getMessage());
            }
        }
        
        private void sendEmail(String subject, String body) {
            System.out.println("📧 Email would be sent to: " + emailTo);
            // Implement email sending with JavaMail if needed
        }
        
        private String formatTime(long timestamp) {
            return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }
    
    // ===============================
    // NEWS FILTER
    // ===============================
    public static class NewsFilter {
        public static boolean shouldTrade(String symbol, LocalDateTime time) {
            // High impact news events calendar
            // Return false if major news event is within 1 hour
            
            // Example: Avoid trading during US NFP (first Friday of month, 13:30 UTC)
            LocalDateTime nfpTime = getNextNFPTime();
            long minutesUntilNFP = java.time.temporal.ChronoUnit.MINUTES.between(time, nfpTime);
            
            if (minutesUntilNFP >= -60 && minutesUntilNFP <= 60) {
                System.out.println("⚠️  Avoiding trade: US NFP within 1 hour");
                return false;
            }
            
            // Add more event checks as needed
            return true;
        }
        
        private static LocalDateTime getNextNFPTime() {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
            // NFP is first Friday of month at 13:30 UTC
            LocalDateTime nfp = LocalDateTime.of(
                now.getYear(), now.getMonth(), 1, 13, 30
            );
            
            // Find first Friday
            while (nfp.getDayOfWeek() != java.time.DayOfWeek.FRIDAY) {
                nfp = nfp.plusDays(1);
            }
            
            if (nfp.isBefore(now)) {
                nfp = nfp.plusMonths(1);
                while (nfp.getDayOfWeek() != java.time.DayOfWeek.FRIDAY) {
                    nfp = nfp.plusDays(1);
                }
            }
            
            return nfp;
        }
    }
    
    // ===============================
    // BOT STATE MANAGER
    // ===============================
    public static class BotState {
        public Queue<BotSignal> signalQueue = new LinkedList<>();
        public Map<Integer, Position> openPositions = new ConcurrentHashMap<>();
        public List<Position> closedPositions = Collections.synchronizedList(new ArrayList<>());
        public Notifier notifier;
        public double accountBalance = 10000;
        public int totalTrades = 0;
        public int winTrades = 0;
        public double totalPnL = 0;
        public volatile boolean active = true;
        public volatile boolean paused = false;
        public double riskPercent = 0.02;
        public String pythonSignalServiceUrl;
        public HttpServer apiServer;
        private ScheduledExecutorService signalExecutor;
        private final Deque<String> recentLogs = new ArrayDeque<>();
        private static final int MAX_LOG_LINES = 100;
        
        public BotState(Notifier notif) {
            notifier = notif;
            pythonSignalServiceUrl = System.getenv(PYTHON_SIGNAL_SERVICE_URL_ENV);
            if (pythonSignalServiceUrl == null || pythonSignalServiceUrl.isBlank()) {
                pythonSignalServiceUrl = DEFAULT_PYTHON_SIGNAL_SERVICE_URL;
            }
        }
        
        public synchronized void log(String message) {
            String timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String entry = "[" + timestamp + "] " + message;
            System.out.println(entry);
            recentLogs.addLast(entry);
            while (recentLogs.size() > MAX_LOG_LINES) {
                recentLogs.removeFirst();
            }
        }

        public synchronized void addSignal(BotSignal signal) {
            if (!active) {
                log("❌ Signal rejected: bot is stopped.");
                return;
            }
            if (paused) {
                log("❌ Signal rejected: bot is paused.");
                return;
            }
            if (isForexMarketClosed()) {
                log("❌ Signal rejected: Forex market is currently closed.");
                return;
            }

            if (!NewsFilter.shouldTrade(signal.symbol, LocalDateTime.now())) {
                log("❌ Signal rejected by news filter");
                return;
            }
            
            signalQueue.offer(signal);
            notifier.sendSignalAlert(signal);
            log("✅ Signal queued: " + signal.direction + " " + signal.symbol);
        }

        public void startSignalProcessor() {
            if (signalExecutor != null && !signalExecutor.isShutdown()) {
                return;
            }
            signalExecutor = Executors.newSingleThreadScheduledExecutor();
            signalExecutor.scheduleAtFixedRate(() -> {
                if (!active || paused) {
                    return;
                }
                BotSignal signal = signalQueue.poll();
                if (signal != null) {
                    log("🔄 Processing queued MT5 signal: " + signal.direction + " " + signal.symbol);
                    openPosition(signal);
                }
            }, 0, 5, TimeUnit.SECONDS);
        }
        
        public synchronized void openPosition(BotSignal signal) {
            double risk = Math.abs(signal.entry - signal.stopLoss);
            double riskAmount = accountBalance * riskPercent;
            double lotSize = riskAmount / (risk * 100000);
            
            Position pos = new Position(signal.ticketNumber, signal.symbol, signal.direction,
                                       signal.entry, signal.stopLoss, signal.takeProfit, lotSize);
            openPositions.put(pos.ticket, pos);
            totalTrades++;
            
            System.out.println("📈 Position OPENED - Ticket: " + pos.ticket);
            notifier.sendPositionUpdate(pos);
        }
        
        public synchronized void closePosition(int ticket, double closePrice, String reason) {
            Position pos = openPositions.remove(ticket);
            if (pos != null) {
                pos.currentPrice = closePrice;
                pos.closeTime = System.currentTimeMillis();
                pos.status = "CLOSED";
                pos.updatePrice(closePrice);
                
                closedPositions.add(pos);
                totalPnL += pos.pnl;
                accountBalance += pos.pnl / 100; // Convert pips to dollars
                
                // Sync with Mobile App
                MobileSignalBridge.sendBalanceUpdate("enohkbosire@gmail.com", accountBalance);

                if (pos.pnl > 0) winTrades++;
                
                System.out.println("📉 Position CLOSED - Ticket: " + ticket + 
                                 " | P&L: $" + String.format("%.2f", pos.pnl / 100) +
                                 " | Reason: " + reason);
                notifier.sendPositionUpdate(pos);
            }
        }
        
        public double getWinRate() {
            return totalTrades > 0 ? (winTrades * 100.0 / totalTrades) : 0;
        }
        
        public double getProfitFactor() {
            double grossProfit = closedPositions.stream()
                .filter(p -> p.pnl > 0)
                .mapToDouble(p -> p.pnl)
                .sum();
            
            double grossLoss = closedPositions.stream()
                .filter(p -> p.pnl < 0)
                .mapToDouble(p -> Math.abs(p.pnl))
                .sum();
            
            return grossLoss > 0 ? grossProfit / grossLoss : 0;
        }

        public synchronized void pauseTrading() {
            if (!active) {
                log("⚠️ Cannot pause because bot is stopped.");
                return;
            }
            paused = true;
            log("⏸ Bot paused.");
        }

        public synchronized void resumeTrading() {
            if (!active) {
                log("⚠️ Cannot resume because bot is stopped.");
                return;
            }
            paused = false;
            log("▶ Bot resumed.");
        }

        public synchronized void stopTrading() {
            paused = true;
            active = false;
            signalQueue.clear();
            if (signalExecutor != null) {
                signalExecutor.shutdownNow();
                signalExecutor = null;
            }
            log("⛔ Bot stopped and signal queue cleared.");
        }

        public synchronized void closeAllPositions(String reason) {
            List<Integer> tickets = new ArrayList<>(openPositions.keySet());
            for (int ticket : tickets) {
                Position pos = openPositions.get(ticket);
                if (pos != null) {
                    closePosition(ticket, pos.currentPrice, reason);
                }
            }
            log("⚠️ All open positions closed: " + reason);
        }

        public synchronized void updatePythonSignalServiceUrl(String url) {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("pythonSignalServiceUrl cannot be blank");
            }
            pythonSignalServiceUrl = url;
            log("⚙️ Python signal service URL updated to " + url);
        }

        public synchronized String getPythonSignalServiceUrl() {
            return pythonSignalServiceUrl;
        }

        public synchronized String getStatusJson() {
            return String.format("{\"active\":%b,\"paused\":%b,\"balance\":%.2f,\"openPositions\":%d,\"closedPositions\":%d,\"winRate\":%.2f,\"riskPercent\":%.4f,\"pythonSignalServiceUrl\":\"%s\"}",
                active, paused, accountBalance, openPositions.size(), closedPositions.size(), getWinRate(), riskPercent, pythonSignalServiceUrl);
        }

        public synchronized String getLogsJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (String log : recentLogs) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(log.replace("\"", "\\\"")).append("\"");
            }
            sb.append("]");
            return sb.toString();
        }
    }
    
    // ===============================
    // REST API SERVER
    // ===============================
    public static void startAPIServer(BotState botState) throws IOException {
        startAPIServer(botState, PORT);
    }

    public static void startAPIServer(BotState botState, int port) throws IOException {
        HttpServer server = null;
        int requestedPort = port;
        while (server == null) {
            try {
                server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            } catch (BindException e) {
                System.out.println("⚠️ Port " + port + " is already in use. Trying next available port...");
                port++;
                if (port > DEFAULT_PORT + 10) {
                    throw new IOException("Unable to bind ForexBot server to any port between " + DEFAULT_PORT + " and " + (DEFAULT_PORT + 10), e);
                }
            }
        }

        // Signal submission endpoint
        server.createContext("/api/signal", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!exchange.getRequestMethod().equals("POST")) {
                    sendResponse(exchange, 405, "Method not allowed");
                    return;
                }
                if (!isAuthorized(exchange)) {
                    sendResponse(exchange, 401, "Unauthorized");
                    return;
                }
                
                try {
                    if (isForexMarketClosed()) {
                        sendResponse(exchange, 503,
                            "{\"error\":\"Forex market is currently closed. Signals are accepted from Sunday 22:00 UTC to Friday 22:00 UTC.\"}");
                        return;
                    }

                    // Parse JSON signal
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    Map<String, Object> data = parseJSON(body);
                    validateSignalPayload(data);
                    
                    BotSignal signal = new BotSignal(
                        (String) data.get("symbol"),
                        (String) data.get("direction"),
                        Double.parseDouble(data.get("entry").toString()),
                        Double.parseDouble(data.get("stopLoss").toString()),
                        Double.parseDouble(data.get("takeProfit").toString()),
                        Double.parseDouble(data.get("confidence").toString()),
                        Double.parseDouble(data.get("signalStrength").toString()),
                        (String) data.get("reason")
                    );
                    
                    botState.addSignal(signal);
                    sendResponse(exchange, 200, "{\"status\":\"accepted\",\"ticket\":" + signal.ticketNumber + "}");
                    
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            }
        });
        
        server.createContext("/api/start", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }
            botState.resumeTrading();
            sendResponse(exchange, 200, "{\"status\":\"started\"}");
        });

        server.createContext("/api/pause", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }
            botState.pauseTrading();
            sendResponse(exchange, 200, "{\"status\":\"paused\"}");
        });

        server.createContext("/api/stop", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }
            botState.stopTrading();
            if (botState.apiServer != null) {
                botState.apiServer.stop(0);
            }
            sendResponse(exchange, 200, "{\"status\":\"stopped\"}");
        });

        server.createContext("/api/close-all", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }
            botState.closeAllPositions("Emergency close requested");
            sendResponse(exchange, 200, "{\"status\":\"closed_all\"}");
        });

        server.createContext("/api/status", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }
            sendResponse(exchange, 200, botState.getStatusJson());
        });

        server.createContext("/api/positions", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }
            String json = "{\"open\":" + botState.openPositions.size() + 
                         ",\"closed\":" + botState.closedPositions.size() +
                         ",\"balance\":" + botState.accountBalance +
                         ",\"winRate\":" + String.format("%.2f", botState.getWinRate()) +
                         ",\"totalTrades\":" + botState.totalTrades + "}";
            sendResponse(exchange, 200, json);
        });

        server.createContext("/api/balance", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }
            String json = String.format("{\"balance\":%.2f,\"riskPercent\":%.4f}", botState.accountBalance, botState.riskPercent);
            sendResponse(exchange, 200, json);
        });

        server.createContext("/api/logs", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }
            sendResponse(exchange, 200, botState.getLogsJson());
        });

        server.createContext("/api/risk", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }
            try {
                String body = new String(exchange.getRequestBody().readAllBytes());
                Map<String, Object> data = parseJSON(body);
                if (!data.containsKey("riskPercent")) {
                    throw new IllegalArgumentException("Missing riskPercent");
                }
                double riskPercent = Double.parseDouble(data.get("riskPercent").toString());
                if (riskPercent <= 0 || riskPercent > 0.2) {
                    throw new IllegalArgumentException("riskPercent must be between 0 and 0.2");
                }
                botState.riskPercent = riskPercent;
                botState.log("⚙️ Risk percent updated to " + (riskPercent * 100) + "%");
                sendResponse(exchange, 200, "{\"status\":\"risk_updated\",\"riskPercent\":" + riskPercent + "}");
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
            }
        });

        server.createContext("/api/config", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                if (!isAuthorized(exchange)) {
                    sendResponse(exchange, 401, "Unauthorized");
                    return;
                }
                sendResponse(exchange, 200, "{\"pythonSignalServiceUrl\":\"" + botState.getPythonSignalServiceUrl().replace("\"", "\\\"") + "\"}");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }
            try {
                String body = new String(exchange.getRequestBody().readAllBytes());
                Map<String, Object> data = parseJSON(body);
                if (!data.containsKey("pythonSignalServiceUrl")) {
                    throw new IllegalArgumentException("Missing pythonSignalServiceUrl");
                }
                botState.updatePythonSignalServiceUrl(data.get("pythonSignalServiceUrl").toString());
                sendResponse(exchange, 200, "{\"status\":\"config_updated\"}");
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
            }
        });

        server.createContext("/api/fetch-signals", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }
            try {
                int count = fetchSignalsFromPythonService(botState);
                sendResponse(exchange, 200, "{\"status\":\"signals_fetched\",\"count\":" + count + "}");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
            }
        });

        // Dashboard endpoint
        server.createContext("/api/dashboard", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String html = buildDashboardHTML(botState);
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, html.length());
                exchange.getResponseBody().write(html.getBytes());
                exchange.close();
            }
        });
        
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        botState.apiServer = server;
        botState.startSignalProcessor();
        if (requestedPort != port) {
            System.out.println("✅ ForexBot API Server started on http://localhost:" + port + " (requested " + requestedPort + ", bound after trying ports " + requestedPort + "-" + port + ").");
        } else {
            System.out.println("✅ ForexBot API Server started on http://localhost:" + port);
        }
        System.out.println("📊 Dashboard: http://localhost:" + port + "/api/dashboard");
        for (String url : getLocalNetworkDashboardUrls(port)) {
            System.out.println("📱 Local network dashboard: " + url);
        }
    }
    
    private static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.close();
    }

    private static boolean isAuthorized(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        return auth != null && auth.equals("Bearer " + API_KEY);
    }

    private static List<String> getLocalNetworkDashboardUrls(int port) {
        List<String> urls = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface netIf = interfaces.nextElement();
                if (!netIf.isUp() || netIf.isLoopback() || netIf.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = netIf.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        urls.add("http://" + addr.getHostAddress() + ":" + port + "/api/dashboard");
                    }
                }
            }
        } catch (SocketException e) {
            // Ignore local network discovery failures
        }
        return urls;
    }
    
    private static Map<String, Object> parseJSON(String json) {
        // Simple JSON parser
        Map<String, Object> map = new HashMap<>();
        String[] pairs = json.replace("{", "").replace("}", "").split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            String key = kv[0].replace("\"", "").trim();
            String value = kv[1].replace("\"", "").trim();
            map.put(key, value);
        }
        return map;
    }

    private static int fetchSignalsFromPythonService(BotState botState) throws IOException {
        String endpoint = botState.getPythonSignalServiceUrl();
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Python signal service URL is not configured.");
        }
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        int status = conn.getResponseCode();
        if (status != 200) {
            throw new IOException("Python signal service returned HTTP " + status);
        }
        String body;
        try (InputStream in = conn.getInputStream()) {
            body = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        List<BotSignal> signals = parseSignalsFromJson(body);
        for (BotSignal signal : signals) {
            botState.addSignal(signal);
        }
        return signals.size();
    }

    private static List<BotSignal> parseSignalsFromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rawList;
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) {
            Map<String, Object> wrapper = GSON.fromJson(trimmed, new TypeToken<Map<String, Object>>(){}.getType());
            Object signalArray = wrapper.get("signals");
            if (signalArray == null) {
                throw new IllegalArgumentException("JSON must contain a signals array or be a top-level array.");
            }
            rawList = GSON.fromJson(GSON.toJson(signalArray), new TypeToken<List<Map<String, Object>>>(){}.getType());
        } else {
            rawList = GSON.fromJson(trimmed, new TypeToken<List<Map<String, Object>>>(){}.getType());
        }
        List<BotSignal> result = new ArrayList<>();
        for (Map<String, Object> data : rawList) {
            validateSignalPayload(data);
            BotSignal signal = new BotSignal(
                data.get("symbol").toString(),
                data.get("direction").toString(),
                Double.parseDouble(data.get("entry").toString()),
                Double.parseDouble(data.get("stopLoss").toString()),
                Double.parseDouble(data.get("takeProfit").toString()),
                Double.parseDouble(data.get("confidence").toString()),
                Double.parseDouble(data.get("signalStrength").toString()),
                data.get("reason").toString()
            );
            result.add(signal);
        }
        return result;
    }

    private static void validateSignalPayload(Map<String, Object> data) {
        String[] required = {"symbol", "direction", "entry", "stopLoss", "takeProfit", "confidence", "signalStrength", "reason"};
        for (String field : required) {
            if (!data.containsKey(field) || data.get(field) == null || data.get(field).toString().trim().isEmpty()) {
                throw new IllegalArgumentException("Missing required field: " + field);
            }
        }

        String direction = data.get("direction").toString().trim().toUpperCase();
        if (!direction.equals("BUY") && !direction.equals("SELL")) {
            throw new IllegalArgumentException("direction must be BUY or SELL");
        }
    }
    
    private static String buildDashboardHTML(BotState state) {
        String marketStatus = isForexMarketClosed() ? "Closed" : "Open";
        String statusClass = isForexMarketClosed() ? "bad" : "good";
        return "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
               "<title>ForexBot Dashboard</title>" +
               "<style>body{font-family:Arial,sans-serif;margin:0;padding:0;background:#101820;color:#f7f7f7;}" +
               "header{background:#1f4287;padding:18px;text-align:center;}" +
               "main{padding:16px;}" +
               ".card{background:#181818;border:1px solid #2e2e2e;border-radius:14px;padding:16px;margin-bottom:16px;}" +
               ".button{display:inline-block;width:48%;margin:4px 1%;padding:14px;text-align:center;border:none;border-radius:12px;font-size:16px;color:#fff;cursor:pointer;}" +
               ".start{background:#00b894;} .pause{background:#fdcb6e;} .stop{background:#d63031;} .closeall{background:#6c5ce7;}" +
               ".metric{margin-bottom:10px;} .small{font-size:0.92em;color:#b2bec3;}" +
               "input,button{font-size:16px;} .row{display:flex;flex-wrap:wrap;}" +
               "textarea{width:100%;height:180px;background:#0d1117;color:#fff;border:1px solid #2e2e2e;border-radius:10px;padding:10px;resize:none;}</style></head><body>" +
               "<header><h1>ForexBot Control</h1><p class='small'>Local control panel for bot state and risk settings</p></header>" +
               "<main><div class='card' style='border-left:4px solid #f39c12;'><h2>Notice</h2><p class='small'>This dashboard runs over HTTP only. Use the exact URL shown in the bot console and accept the insecure warning if your phone browser shows it. Do not use HTTPS.</p></div><div class='card'><h2>Status</h2>" +
               "<div class='metric'>Market: <strong class='" + statusClass + "'>" + marketStatus + "</strong></div>" +
               "<div class='metric'>Balance: <strong>$" + String.format("%.2f", state.accountBalance) + "</strong></div>" +
               "<div class='metric'>Open Positions: <strong>" + state.openPositions.size() + "</strong></div>" +
               "<div class='metric'>Win Rate: <strong>" + String.format("%.2f%%", state.getWinRate()) + "</strong></div>" +
               "<div class='metric'>Risk: <strong>" + String.format("%.2f%%", state.riskPercent * 100) + "</strong></div>" +
               "<div class='metric'>Python Signal Service: <strong>" + state.getPythonSignalServiceUrl().replace("\"", "\\\"") + "</strong></div>" +
               "</div><div class='card'><h2>Controls</h2>" +
               "<div class='row'><button class='button start' onclick='postAction(\"/api/start\")'>▶ Start</button>" +
               "<button class='button pause' onclick='postAction(\"/api/pause\")'>⏸ Pause</button></div>" +
               "<div class='row'><button class='button stop' onclick='postAction(\"/api/stop\")'>🛑 Stop</button>" +
               "<button class='button closeall' onclick='postAction(\"/api/close-all\")'>⚠ Close All</button></div>" +               "<div class='row'><button class='button start' style='width:100%;margin-top:4px;' onclick='postAction(\"/api/fetch-signals\")'>🔄 Fetch Python Signals</button></div>" +               "</div><div class='card'><h2>Settings</h2>" +
               "<label class='metric'>Risk %<br><input id='riskInput' type='number' step='0.01' min='0.01' max='0.20' value='" + String.format("%.2f", state.riskPercent * 100) + "'></label>" +
               "<button class='button start' style='width:100%;' onclick='updateRisk()'>Save Risk</button>" +
               "</div><div class='card'><h2>Logs</h2>" +
               "<textarea id='logs' readonly>Loading logs...</textarea></div></main>" +
               "<script>const apiKey='" + API_KEY + "';function postAction(path){fetch(path,{method:'POST',headers:{'Content-Type':'application/json','Authorization':'Bearer '+apiKey}}).then(r=>r.text()).then(t=>alert(t)).catch(e=>alert('Error:'+e));}function updateRisk(){const value=parseFloat(document.getElementById('riskInput').value)/100;fetch('/api/risk',{method:'POST',headers:{'Content-Type':'application/json','Authorization':'Bearer '+apiKey},body:JSON.stringify({riskPercent:value})}).then(r=>r.text()).then(t=>alert(t)).catch(e=>alert('Error:'+e));}function refreshLogs(){fetch('/api/logs',{headers:{Authorization:'Bearer '+apiKey}}).then(r=>r.json()).then(js=>{document.getElementById('logs').value=js.join('\n');}).catch(e=>document.getElementById('logs').value='Unable to load logs';);}window.onload=refreshLogs;</script></body></html>";
    }

    private static String buildOpenPositionsHTML(BotState state) {
        if (state.openPositions.isEmpty()) {
            return "<p>No open positions</p>";
        }

        StringBuilder sb = new StringBuilder();
        for (Position p : state.openPositions.values()) {
            sb.append(String.format(
                "<p>%s %s - Entry: %.5f | Current: %.5f | P&L: $%.2f</p>",
                p.symbol, p.type, p.entryPrice, p.currentPrice, p.pnl / 100
            ));
        }
        return sb.toString();
    }
    
    public static void main(String[] args) throws IOException {
        System.out.println("🤖 ForexBot Starting...");
        System.out.println("════════════════════════════════════════════════");
        
        // Initialize notifier (configure with your Telegram/Email)
        Notifier notifier = new Notifier(
            System.getenv("TELEGRAM_BOT_TOKEN"),
            System.getenv("TELEGRAM_CHAT_ID"),
            System.getenv("EMAIL_TO")
        );
        
        // Initialize bot state
        BotState botState = new BotState(notifier);
        
        // Start API server
        startAPIServer(botState);
        
        System.out.println("\n💡 Bot is ready to receive signals from MT5");
        System.out.println("📝 Configure MT5 Expert Advisor to send signals to: http://localhost:" + PORT + "/api/signal");
        System.out.println("🔑 Use API Key: " + API_KEY);
        System.out.println("════════════════════════════════════════════════\n");
        
        // Keep server running
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

