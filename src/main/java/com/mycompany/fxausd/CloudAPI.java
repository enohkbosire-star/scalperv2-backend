package com.mycompany.fxausd;

import static spark.Spark.*;
import com.google.gson.Gson;
import java.sql.*;
import java.util.*;
import java.util.Date;
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URL;

public class CloudAPI {

    private static final Gson gson = new Gson();
    private static final Map<String, String> otpStorage = new HashMap<>();
    private static boolean isDashboardActive = true;
    private static boolean isBotActive = true;
    private static String botStatus = "INITIALIZING...";
    private static String lastSignalInsight = "Connecting to neural networks...";

    public static void updateBotStatus(String status, String insight) {
        botStatus = status;
        lastSignalInsight = insight;
    }

    // GMAIL CONFIGURATION (Use App Password)
    private static final String SENDER_EMAIL = "enohkbosire@gmail.com";
    private static final String APP_PASSWORD = "fifidlridfmzygfs";

    public static void start() {
        // Detect Render port or fallback to 4567
        String portStr = System.getenv("PORT");
        int portNumber = (portStr != null && !portStr.isEmpty()) ? Integer.parseInt(portStr) : 4567;
        
        port(portNumber);
        ipAddress("0.0.0.0");

        // Create uploads directory
        new File("uploads").mkdirs();
        staticFiles.externalLocation("uploads");

        // CORS
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "*");
            res.header("Access-Control-Allow-Headers", "*");
            res.type("application/json"); 
        });

        initDatabase();

        // =========================
        // CORE BOT FEED & PULSE
        // =========================
        get("/api/bot-pulse", (req, res) -> {
            Map<String, Object> pulse = new HashMap<>();
            pulse.put("status", botStatus);
            pulse.put("insight", lastSignalInsight);
            pulse.put("market_open", !Fxausd.isForexMarketClosed());
            pulse.put("timestamp", System.currentTimeMillis());
            return gson.toJson(pulse);
        });

        get("/api/intelligence", (req, res) -> {
            Map<String, Object> intel = new HashMap<>();
            intel.put("session", "LONDON/NY OVERLAP");
            intel.put("bias", "INSTITUTIONAL BULLISH");
            intel.put("volatility", "HIGH");
            return gson.toJson(intel);
        });

        post("/request-otp", (req, res) -> {
            Map<String, String> data = gson.fromJson(req.body(), Map.class);
            String email = data.get("email");
            if (email == null || email.isBlank()) return gson.toJson(Map.of("status", "fail", "message", "Email required"));

            String otp = String.format("%06d", new Random().nextInt(999999));
            otpStorage.put(email.toLowerCase().trim(), otp);

            System.out.println("📧 OTP Request for " + email + ": " + otp);
            
            try {
                sendEmail(email, "FXAUSD Verification Code", "Your secure verification code is: " + otp);
                return gson.toJson(Map.of("status", "success", "message", "OTP sent successfully"));
            } catch (Exception e) {
                e.printStackTrace();
                return gson.toJson(Map.of("status", "error", "message", "Failed to send email"));
            }
        });

        post("/verify-otp", (req, res) -> {
            Map<String, String> data = gson.fromJson(req.body(), Map.class);
            String email = data.get("email") != null ? data.get("email").toLowerCase().trim() : "";
            String otp = data.get("otp");
            
            if (otpStorage.containsKey(email) && otpStorage.get(email).equals(otp)) {
                otpStorage.remove(email);
                return gson.toJson(Map.of("status", "success"));
            }
            return gson.toJson(Map.of("status", "fail", "message", "Invalid OTP"));
        });

        // =========================
        // M-PESA PAYMENTS
        // =========================
        post("/wallet/mpesa-deposit", (req, res) -> {
            Map<String, Object> data = gson.fromJson(req.body(), Map.class);
            String phone = (String) data.get("phone");
            System.out.println("💰 M-Pesa Deposit Request for " + phone);
            return gson.toJson(Map.of("status", "success", "message", "STK Push sent to " + phone));
        });

        post("/wallet/mpesa-withdraw", (req, res) -> {
            Map<String, Object> data = gson.fromJson(req.body(), Map.class);
            String email = (String) data.get("email");
            double amount = Double.parseDouble(data.get("amount").toString());
            try (Connection conn = connect()) {
                PreparedStatement ps = conn.prepareStatement("SELECT balance FROM users WHERE email=?");
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getDouble("balance") >= amount) {
                    PreparedStatement update = conn.prepareStatement("UPDATE users SET balance = balance - ? WHERE email=?");
                    update.setDouble(1, amount);
                    update.setString(2, email);
                    update.executeUpdate();
                    return gson.toJson(Map.of("status", "success", "message", "Withdrawal initiated"));
                } else {
                    return gson.toJson(Map.of("status", "fail", "message", "Insufficient balance"));
                }
            } catch (Exception e) {
                return gson.toJson(Map.of("status", "error"));
            }
        });

        // =========================
        // AI ASSISTANT
        // =========================
        post("/ai/chat", (req, res) -> {
            Map<String, String> data = gson.fromJson(req.body(), Map.class);
            String userMsg = data.get("message").toLowerCase();
            String response = "I am the FXAUSD AI-1 Neural Assistant. Query acknowledged: " + userMsg;
            if (userMsg.contains("gold") || userMsg.contains("xauusd")) {
                response = "⚡ [Neural Structural Audit] XAUUSD exhibits institutional liquidity above 2058. Market Bias: Bullish.";
            } else if (userMsg.contains("risk")) {
                response = "🛡️ [Elite Risk Protocol] Adhere to the 1.5% fixed fractional model. Equity * RiskRatio / (SL * PipValue).";
            }
            return gson.toJson(Map.of("response", response));
        });

        // =========================
        // NOTIFICATIONS
        // =========================
        get("/notifications", (req, res) -> {
            res.type("application/json");
            List<Map<String, Object>> notifications = new ArrayList<>();
            try (Connection conn = connect()) {
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM notifications ORDER BY id DESC LIMIT 50");
                while (rs.next()) {
                    notifications.add(Map.of(
                            "id", rs.getInt("id"),
                            "title", rs.getString("title"),
                            "message", rs.getString("message"),
                            "timestamp", rs.getTimestamp("timestamp").toString()
                    ));
                }
            } catch (Exception e) { e.printStackTrace(); }
            return gson.toJson(notifications);
        });

        // =========================
        // SIGNALS
        // =========================
        get("/signals", (req, res) -> {
            res.type("application/json");
            List<Map<String, Object>> signals = new ArrayList<>();
            try (Connection conn = connect()) {
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM signals ORDER BY id DESC");
                while (rs.next()) {
                    signals.add(Map.of(
                            "id", rs.getInt("id"),
                            "pair", rs.getString("pair"),
                            "action", rs.getString("action"),
                            "entry", rs.getDouble("entry_price"),
                            "tp", rs.getDouble("tp"),
                            "sl", rs.getDouble("sl"),
                            "confidence", rs.getDouble("confidence"),
                            "strength", rs.getDouble("strength")
                    ));
                }
            } catch (Exception e) { e.printStackTrace(); }
            return gson.toJson(signals);
        });

        post("/signals/add", (req, res) -> {
            if (!isBotActive) return gson.toJson(Map.of("status", "error", "message", "Bot Engine is stopped"));
            Map<String, Object> data = gson.fromJson(req.body(), Map.class);
            try (Connection conn = connect()) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO signals(pair, action, entry_price, tp, sl, confidence, strength) VALUES (?, ?, ?, ?, ?, ?, ?)");
                ps.setString(1, (String) data.get("pair"));
                ps.setString(2, (String) data.get("action"));
                ps.setDouble(3, Double.parseDouble(data.get("entry").toString()));
                ps.setDouble(4, Double.parseDouble(data.get("tp").toString()));
                ps.setDouble(5, Double.parseDouble(data.get("sl").toString()));
                ps.setDouble(6, data.containsKey("confidence") ? Double.parseDouble(data.get("confidence").toString()) : 0.0);
                ps.setDouble(7, data.containsKey("strength") ? Double.parseDouble(data.get("strength").toString()) : 0.0);
                ps.executeUpdate();
                return gson.toJson(Map.of("status", "success"));
            } catch (Exception e) { return gson.toJson(Map.of("status", "error")); }
        });

        // =========================
        // LOGIN
        // =========================
        post("/login", (req, res) -> {
            Map<String, String> data = gson.fromJson(req.body(), Map.class);
            String email = data.get("email") != null ? data.get("email").trim() : "";
            String password = data.get("password") != null ? data.get("password").trim() : "";

            System.out.println("🔐 Login Attempt: " + email);

            try (Connection conn = connect()) {
                PreparedStatement checkUser = conn.prepareStatement("SELECT * FROM users WHERE email=?");
                checkUser.setString(1, email);
                ResultSet rs = checkUser.executeQuery();

                if (rs.next()) {
                    if (rs.getString("password").equals(password)) {
                        Map<String, Object> resp = new HashMap<>();
                        resp.put("status", "success");
                        resp.put("email", email);
                        resp.put("name", rs.getString("name"));
                        resp.put("user_id", "FX-" + rs.getInt("id"));
                        resp.put("is_admin", rs.getBoolean("is_admin"));
                        resp.put("is_approved", rs.getBoolean("is_approved"));
                        System.out.println("✅ Login Success: " + email);
                        return gson.toJson(resp);
                    } else {
                        return gson.toJson(Map.of("status", "fail", "message", "Incorrect password"));
                    }
                } else {
                    return gson.toJson(Map.of("status", "fail", "message", "Account not found"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                return gson.toJson(Map.of("status", "error", "message", e.getMessage()));
            }
        });

        // =========================
        // COMMUNITY & CHAT
        // =========================
        get("/statuses", (req, res) -> {
            List<Map<String, Object>> list = new ArrayList<>();
            try (Connection conn = connect()) {
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM statuses ORDER BY id DESC LIMIT 50");
                while (rs.next()) list.add(Map.of("id", rs.getInt("id"), "name", rs.getString("name"), "content", rs.getString("content"), "likes", rs.getInt("likes")));
            } catch (Exception e) {}
            return gson.toJson(list);
        });

        post("/statuses/post", (req, res) -> {
            Map<String, String> data = gson.fromJson(req.body(), Map.class);
            try (Connection conn = connect()) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO statuses(email, name, content) VALUES (?, ?, ?)");
                ps.setString(1, data.get("email")); ps.setString(2, data.get("name")); ps.setString(3, data.get("content"));
                ps.executeUpdate();
                return gson.toJson(Map.of("status", "success"));
            } catch (Exception e) { return gson.toJson(Map.of("status", "error")); }
        });

        get("/messages", (req, res) -> {
            List<Map<String, String>> messages = new ArrayList<>();
            try (Connection conn = connect()) {
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM chat_messages ORDER BY id DESC LIMIT 50");
                while (rs.next()) messages.add(0, Map.of("user", rs.getString("username"), "text", rs.getString("message_text"), "type", rs.getString("message_type")));
            } catch (Exception e) {}
            return gson.toJson(messages);
        });

        post("/send-message", (req, res) -> {
            Map<String, String> data = gson.fromJson(req.body(), Map.class);
            try (Connection conn = connect()) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO chat_messages(username, message_text, message_type) VALUES (?, ?, ?)");
                ps.setString(1, data.get("user")); ps.setString(2, data.get("text")); ps.setString(3, data.getOrDefault("type", "text"));
                ps.executeUpdate();
                return gson.toJson(Map.of("status", "success"));
            } catch (Exception e) { return gson.toJson(Map.of("status", "error")); }
        });

        get("/user/journal", (req, res) -> {
            List<Map<String, String>> entries = new ArrayList<>();
            try (Connection conn = connect()) {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM journals WHERE email=? ORDER BY id DESC");
                ps.setString(1, req.queryParams("email"));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) entries.add(Map.of("content", rs.getString("content"), "timestamp", rs.getTimestamp("timestamp").toString()));
            } catch (Exception e) {}
            return gson.toJson(entries);
        });

        post("/user/journal", (req, res) -> {
            Map<String, String> data = gson.fromJson(req.body(), Map.class);
            try (Connection conn = connect()) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO journals(email, content) VALUES (?, ?)");
                ps.setString(1, data.get("email")); ps.setString(2, data.get("content"));
                ps.executeUpdate();
                return gson.toJson(Map.of("status", "success"));
            } catch (Exception e) { return gson.toJson(Map.of("status", "error")); }
        });

        post("/user/update-balance", (req, res) -> {
            Map<String, Object> data = gson.fromJson(req.body(), Map.class);
            String email = (String) data.get("email");
            double balance = Double.parseDouble(data.get("balance").toString());
            String column = (data.containsKey("type") && "bot".equals(data.get("type"))) ? "bot_balance" : "balance";
            try (Connection conn = connect()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE users SET " + column + "=? WHERE email=?");
                ps.setDouble(1, balance); ps.setString(2, email);
                ps.executeUpdate();
                return gson.toJson(Map.of("status", "success"));
            } catch (Exception e) { return gson.toJson(Map.of("status", "error")); }
        });

        notFound((req, res) -> gson.toJson(Map.of("status", 404, "message", "Route not found")));
    }

    public static Connection connect() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection("jdbc:postgresql://ep-summer-cloud-apjyv5uu-pooler.c-7.us-east-1.aws.neon.tech/neondb?sslmode=require&user=neondb_owner&password=npg_jx0YXqbu6sAV");
        } catch (Exception e) { throw new SQLException(e); }
    }

    private static void initDatabase() {
        try (Connection conn = connect()) {
            Statement st = conn.createStatement();
            st.execute("CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, email VARCHAR(100) UNIQUE, name VARCHAR(100), phone VARCHAR(20), password VARCHAR(100), balance DOUBLE PRECISION DEFAULT 50, bot_balance DOUBLE PRECISION DEFAULT 0, is_admin BOOLEAN DEFAULT FALSE, is_approved BOOLEAN DEFAULT TRUE, community_status VARCHAR(20) DEFAULT 'none', profile_pic_url TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS signals (id SERIAL PRIMARY KEY, pair VARCHAR(20), action VARCHAR(10), entry_price DOUBLE PRECISION, tp DOUBLE PRECISION, sl DOUBLE PRECISION, confidence DOUBLE PRECISION, strength DOUBLE PRECISION)");
            st.execute("CREATE TABLE IF NOT EXISTS news (id SERIAL PRIMARY KEY, title TEXT, time VARCHAR(100), impact VARCHAR(20) DEFAULT 'Low', currency VARCHAR(10) DEFAULT 'USD', is_future BOOLEAN DEFAULT FALSE)");
            st.execute("CREATE TABLE IF NOT EXISTS feedbacks (id SERIAL PRIMARY KEY, email VARCHAR(100), feedback TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS notifications (id SERIAL PRIMARY KEY, title VARCHAR(255), message TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS chat_messages (id SERIAL PRIMARY KEY, username VARCHAR(100), message_text TEXT, message_type VARCHAR(20) DEFAULT 'text', media_url TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS journals (id SERIAL PRIMARY KEY, email VARCHAR(100), content TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS statuses (id SERIAL PRIMARY KEY, email VARCHAR(100), name VARCHAR(100), content TEXT, likes INT DEFAULT 0, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS status_likes (status_id INT, email VARCHAR(100), PRIMARY KEY(status_id, email))");
            System.out.println("✅ Database synced with Neon.tech");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void sendEmail(String recipient, String subject, String content) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "FXAUSD ELITE"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            
            // Premium HTML Template
            String htmlContent = "<div style='background:#050811; color:#fff; padding:30px; border-radius:15px; font-family:sans-serif; border:1px solid #D4AF37;'>" +
                                 "<h2 style='color:#D4AF37; margin-top:0;'>FXAUSD INSTITUTIONAL</h2>" +
                                 "<p style='font-size:16px;'>Your secure verification code is:</p>" +
                                 "<div style='background:#111; padding:20px; text-align:center; border-radius:10px; margin:20px 0;'>" +
                                 "<span style='font-size:32px; color:#D4AF37; letter-spacing:10px; font-weight:bold;'>" + content.replaceAll("\\D+", "") + "</span>" +
                                 "</div>" +
                                 "<p style='font-size:12px; color:#64748B; margin-bottom:0;'>Valid for 10 minutes. If you did not request this, please ignore this email.</p>" +
                                 "<p style='font-size:10px; color:#444; text-align:right;'>@Enohk Elite AI Security</p>" +
                                 "</div>";
            
            message.setContent(htmlContent, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("✅ Institutional OTP dispatched to " + recipient);
        } catch (Exception e) { 
            System.err.println("❌ Email delivery failed: " + e.getMessage());
            e.printStackTrace(); 
        }
    }
}
