package com.mycompany.fxausd;

import static spark.Spark.*;
import com.google.gson.Gson;
import java.sql.*;
import java.util.*;

public class FxausdAPI {
    private static final Gson GSON = new Gson();
    private static Connection dbConn;

    public static void start(int port) {
        port(port);
        setupDatabase();

        // 1. MOBILE APP ENDPOINTS
        get("/signals", (req, res) -> {
            res.type("application/json");
            return GSON.toJson(fetchRecentSignals());
        });

        post("/login", (req, res) -> {
            res.type("application/json");
            // Simplified login logic for your trading community
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("user_id", "FX-" + System.currentTimeMillis() % 10000);
            return GSON.toJson(response);
        });

        // 2. INTERNAL BOT ENDPOINT (When bot generates signal, it calls this)
        post("/signals/add", (req, res) -> {
            Map<String, Object> signalData = GSON.fromJson(req.body(), Map.class);
            saveSignalToDB(signalData);
            return "{\"status\":\"saved\"}";
        });

        System.out.println("🚀 Fxausd Spark API running on port " + port);
    }

    private static void setupDatabase() {
        try {
            // Using your Neon.tech Cloud Connection
            String dbUrl = "jdbc:postgresql://ep-summer-cloud-apjyv5uu.c-7.us-east-1.aws.neon.tech/neondb?sslmode=require";
            dbConn = DriverManager.getConnection(dbUrl, "neondb_owner", "npg_jx0YXqbu6sAV");
            System.out.println("✅ API connected to Cloud Database");
        } catch (Exception e) {
            System.err.println("❌ DB Error: " + e.getMessage());
        }
    }

    private static List<Map<String, Object>> fetchRecentSignals() {
        List<Map<String, Object>> signals = new ArrayList<>();
        // In production, you would SELECT from your Neon database here
        return signals;
    }

    private static void saveSignalToDB(Map<String, Object> data) {
        // SQL INSERT logic for Neon.tech
    }
}
