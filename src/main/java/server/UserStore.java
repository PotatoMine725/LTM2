package server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class UserStore {
    private static final String DB_URL = "jdbc:sqlite:ltm2-users.db";

    UserStore() {
        initDatabase();
    }

    boolean register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        String trimmedUsername = username.trim();
        String passwordHash = hash(password.trim());
        String salt = hash(trimmedUsername + ":salt");
        String storedPassword = hash(passwordHash + salt);
        String sql = "INSERT INTO users(username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trimmedUsername);
            statement.setString(2, storedPassword);
            statement.setString(3, salt);
            statement.executeUpdate();
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    boolean authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        String sql = "SELECT password_hash, salt FROM users WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                String storedHash = resultSet.getString("password_hash");
                String salt = resultSet.getString("salt");
                String inputHash = hash(hash(password.trim()) + salt);
                return storedHash.equals(inputHash);
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    private void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT NOT NULL UNIQUE, "
                + "password_hash TEXT NOT NULL, "
                + "salt TEXT NOT NULL)";
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("SQLite JDBC driver not found", ex);
        }
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to initialize user database", ex);
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
