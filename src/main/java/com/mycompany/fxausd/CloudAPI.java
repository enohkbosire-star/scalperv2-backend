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
    private static String lastSignalInsight = "Connecting to liquidity pools...";

    // GMAIL CONFIGURATION (Use App Password)
    private static final String SENDER_EMAIL = "enohkbosire@gmail.com";
    private static final String APP_PASSWORD = "fifidlridfmzygfs";

    public static void updateBotStatus(String status, String insight) {
        botStatus = status;
        lastSignalInsight = insight;
    }

    public static void start() {
        // Detect Render port or fallback to 8888 (matching Fxausd class)
        String portStr = System.getenv("PORT");
        int portNumber = (portStr != null && !portStr.isEmpty()) ? Integer.parseInt(portStr) : 8888;
        
        port(portNumber);
        ipAddress("0.0.0.0");

        // Create uploads directory
        new File("uploads").mkdirs();
        staticFiles.externalLocation("uploads");

        // CORS
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            res.header("Access-Control-Allow-Headers", "*");
            res.type("application/json"); 
        });

        initDatabase();

        // =========================
        // CORE API STATUS
        // =========================
        get("/api/status", (req, res) -> {
            Map<String, Object> status = new HashMap<>();
            status.put("status", "ONLINE");
            status.put("bot_engine", isBotActive ? "ACTIVE" : "STOPPED");
            status.put("database", "CONNECTED");
            return gson.toJson(status);
        });

        get("/api/bot-pulse", (req, res) -> {
            Map<String, Object> pulse = new HashMap<>();
            pulse.put("status", botStatus);
            pulse.put("insight", lastSignalInsight);
            pulse.put("market_open", !Fxausd.isForexMarketClosed());
            pulse.put("timestamp", System.currentTimeMillis());
            return gson.toJson(pulse);
        });

        // =========================
        // AUTH & OTP
        // =========================
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
        // SIGNALS
        // =========================
        get("/signals", (req, res) -> {
            res.type("application/json");
            List<Map<String, Object>> signals = new ArrayList<>();
            try (Connection conn = connect()) {
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM signals ORDER BY id DESC LIMIT 50");
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
                        return gson.toJson(resp);
                    }
                }
            } catch (Exception e) {}
            return gson.toJson(Map.of("status", "fail", "message", "Invalid credentials"));
        });

        get("/user", (req, res) -> {
            String email = req.queryParams("email");
            try (Connection conn = connect()) {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email=?");
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("email", rs.getString("email"));
                    m.put("name", rs.getString("name"));
                    m.put("balance", rs.getDouble("balance"));
                    m.put("is_admin", rs.getBoolean("is_admin"));
                    m.put("user_id", "FX-" + rs.getInt("id"));
                    return gson.toJson(m);
                }
            } catch (Exception e) {}
            return "{}";
        });

        // =========================
        // COMMUNITY & CHAT
        // =========================
        get("/messages", (req, res) -> {
            List<Map<String, String>> messages = new ArrayList<>();
            try (Connection conn = connect()) {
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM chat_messages ORDER BY id DESC LIMIT 50");
                while (rs.next()) {
                    Map<String, String> msg = new HashMap<>();
                    msg.put("user", rs.getString("username"));
                    msg.put("text", rs.getString("message_text"));
                    msg.put("type", rs.getString("message_type"));
                    msg.put("timestamp", rs.getTimestamp("timestamp").toString());
                    messages.add(0, msg);
                }
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

        notFound((req, res) -> gson.toJson(Map.of("status", 404, "message", "Endpoint protected by Sentinel")));
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
            st.execute("CREATE TABLE IF NOT EXISTS chat_messages (id SERIAL PRIMARY KEY, username VARCHAR(100), message_text TEXT, message_type VARCHAR(20) DEFAULT 'text', media_url TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS notifications (id SERIAL PRIMARY KEY, title VARCHAR(255), message TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            // Ensure Admin exists
            PreparedStatement ps = conn.prepareStatement("INSERT INTO users(email, name, password, balance, is_admin, is_approved) VALUES ('enohkbosire@gmail.com', 'Elite Master Enohk', 'Enohk123@', 1000000.0, TRUE, TRUE) ON CONFLICT (email) DO UPDATE SET is_admin=TRUE");
            ps.executeUpdate();
            System.out.println("✅ Institutional database synced");
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
            message.setContent("<div style='background:#050811; color:#fff; padding:30px; border-radius:15px; font-family:sans-serif; border:1px solid #D4AF37;'>" +
                               "<h2 style='color:#D4AF37;'>FXAUSD INSTITUTIONAL</h2>" +
                               "<p>Your secure verification code is:</p>" +
                               "<div style='background:#111; padding:20px; text-align:center; border-radius:10px;'>" +
                               "<span style='font-size:32px; color:#D4AF37; letter-spacing:10px; font-weight:bold;'>" + content.replaceAll("\\D+", "") + "</span>" +
                               "</div>" +
                               "<p style='font-size:12px; color:#64748B; margin-top:20px;'>Valid for 10 minutes. @Enohk Institutional Security</p>" +
                               "</div>", "text/html; charset=utf-8");
            Transport.send(message);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
