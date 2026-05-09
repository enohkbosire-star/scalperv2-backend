
package com.mycompany.fxausd;

import java.sql.*;
import java.util.Scanner;

public class DBConnect {

    public static Connection connect() {
        try {
            Class.forName("org.postgresql.Driver");

            return DriverManager.getConnection(
                "jdbc:postgresql://ep-summer-cloud-apjyv5uu-pooler.c-7.us-east-1.aws.neon.tech/neondb?sslmode=require&user=neondb_owner&password=npg_jx0YXqbu6sAV"
            );

        } catch (Exception e) {
            System.out.println("Database Error: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        try {
            Connection conn = connect();

            // LOGIN
            System.out.print("Username: ");
            String username = input.nextLine();

            System.out.print("Password: ");
            String password = input.nextLine();

            String loginSql =
            "SELECT * FROM users WHERE username=? AND password=?";

            PreparedStatement login = conn.prepareStatement(loginSql);
            login.setString(1, username);
            login.setString(2, password);

            ResultSet rs = login.executeQuery();

            if (rs.next()) {

                int userId = rs.getInt("id");

                System.out.println("Login Successful");
                System.out.println("Welcome " + username);

                // TRADE SECTION
                System.out.print("Pair Name (EURUSD): ");
                String pair = input.nextLine();

                System.out.print("Trade Type (BUY/SELL): ");
                String type = input.nextLine();

                System.out.print("Amount: ");
                double amount = input.nextDouble();

                System.out.print("Entry Price: ");
                double entry = input.nextDouble();

                String sql =
                "INSERT INTO trades(user_id,pair_name,trade_type,amount,entry_price,status) VALUES(?,?,?,?,?,?)";

                PreparedStatement ps =
                conn.prepareStatement(sql);

                ps.setInt(1, userId);
                ps.setString(2, pair);
                ps.setString(3, type);
                ps.setDouble(4, amount);
                ps.setDouble(5, entry);
                ps.setString(6, "OPEN");

                ps.executeUpdate();

                System.out.println("Trade Saved Successfully!");

            } else {
                System.out.println("Invalid Login");
            }

            conn.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}