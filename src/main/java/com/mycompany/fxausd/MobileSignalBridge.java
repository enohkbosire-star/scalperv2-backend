package com.mycompany.fxausd;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MobileSignalBridge {

    private static final String API_URL = "http://192.168.100.38:4567/signals/add";

    public static void sendToMobile(String pair, String action, double entry, double tp, double sl, double confidence, double strength) {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);

                String json = String.format(
                    "{\"pair\": \"%s\", \"action\": \"%s\", \"entry\": %f, \"tp\": %f, \"sl\": %f, \"confidence\": %f, \"strength\": %f}",
                    pair, action, entry, tp, sl, confidence, strength
                );

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    System.out.println("📱 [Mobile Bridge] Signal sent successfully to app: " + pair + " " + action);
                } else {
                    System.err.println("📱 [Mobile Bridge] Failed to send signal. HTTP Code: " + code);
                }
            } catch (Exception e) {
                System.err.println("📱 [Mobile Bridge] Error: " + e.getMessage());
            }
        }).start();
    }

    // Keep compatibility for old calls
    public static void sendToMobile(String pair, String action, double entry, double tp, double sl) {
        sendToMobile(pair, action, entry, tp, sl, 0.0, 0.0);
    }

    public static void sendBalanceUpdate(String email, double balance) {
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.100.38:4567/user/update-balance");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setDoOutput(true);

                // Added type: bot to specify this is the trading bot's account balance
                String json = String.format("{\"email\": \"%s\", \"balance\": %f, \"type\": \"bot\"}", email, balance);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                conn.getResponseCode();
            } catch (Exception ignored) {}
        }).start();
    }
}
