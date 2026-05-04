package server;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

final class UserStore {
    // M4: lưu DB trong ~/.ltm2/ thay vì working directory — giảm rủi ro lộ dữ liệu
    private static final String DB_URL;
    static {
        File dataDir = new File(System.getProperty("user.home"), ".ltm2");
        dataDir.mkdirs();
        DB_URL = "jdbc:sqlite:" + new File(dataDir, "ltm2-users.db").getAbsolutePath();
    }

    UserStore() {
        initDatabase();
    }

    boolean register(String username, String password) {
        if (username == null || password == null) return false;
        String trimmedUsername = username.trim();
        String trimmedPassword = password.trim();
        // M1: giới hạn độ dài và ký tự để tránh DoS và injection
        if (trimmedUsername.length() < 3 || trimmedUsername.length() > 32) return false;
        if (trimmedPassword.length() < 8 || trimmedPassword.length() > 128) return false;
        if (!trimmedUsername.matches("[a-zA-Z0-9_]+")) return false;
        // C2/C3: BCrypt — tự tạo random salt 16 bytes, cost factor 12
        String bcryptHash = BCrypt.withDefaults().hashToString(12, trimmedPassword.toCharArray());
        String sql = "INSERT INTO users(username, password_hash, salt, bcrypt_hash) VALUES (?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trimmedUsername);
            statement.setString(2, "");   // cột cũ — để trống với tài khoản mới
            statement.setString(3, "");   // cột cũ — để trống với tài khoản mới
            statement.setString(4, bcryptHash);
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
        String trimmedUser = username.trim();
        String trimmedPass = password.trim();
        String sql = "SELECT password_hash, salt, bcrypt_hash FROM users WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trimmedUser);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    // L1: constant-time — luôn tính toán dù user không tồn tại
                    BCrypt.verifyer().verify(trimmedPass.toCharArray(),
                            "$2a$12$dummyhashfortimingnormalization000000000000000000000000");
                    return false;
                }
                String bcryptHash = rs.getString("bcrypt_hash");
                // C2/C3: tài khoản đã có BCrypt hash — dùng BCrypt verify
                if (bcryptHash != null && !bcryptHash.isBlank()) {
                    return BCrypt.verifyer().verify(trimmedPass.toCharArray(), bcryptHash).verified;
                }
                // Lazy migration: tài khoản cũ dùng SHA-256 — verify rồi nâng cấp
                String oldHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                String inputHash = hash(hash(trimmedPass) + salt);
                if (oldHash.equals(inputHash)) {
                    // Tự động nâng cấp lên BCrypt
                    upgradePasswordHash(trimmedUser, trimmedPass);
                    return true;
                }
                return false;
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    /** Nâng cấp tài khoản cũ từ SHA-256 lên BCrypt sau khi xác thực thành công */
    private void upgradePasswordHash(String username, String password) {
        String newHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String sql = "UPDATE users SET bcrypt_hash = ?, password_hash = '', salt = '' WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newHash);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    void saveMessage(String sender, String type, String content) {
        String sql = "INSERT INTO messages(sender, type, content, sent_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sender);
            statement.setString(2, type);
            statement.setString(3, content);
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    // Returns list of [type, sender, content, timestamp_ms], oldest first
    List<String[]> getRecentMessages(int limit) {
        String sql = "SELECT type, sender, content, sent_at FROM "
                + "(SELECT type, sender, content, sent_at FROM messages ORDER BY sent_at DESC LIMIT ?) "
                + "ORDER BY sent_at ASC";
        List<String[]> result = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new String[]{
                            rs.getString("type"),
                            rs.getString("sender"),
                            rs.getString("content"),
                            String.valueOf(rs.getLong("sent_at"))
                    });
                }
            }
        } catch (SQLException ignored) {
        }
        return result;
    }

    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("SQLite JDBC driver not found", ex);
        }
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement()) {
            // Tạo bảng users với cột bcrypt_hash mới, giữ cột cũ để backward compat
            statement.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL UNIQUE, "
                    + "password_hash TEXT NOT NULL DEFAULT '', "
                    + "salt TEXT NOT NULL DEFAULT '', "
                    + "bcrypt_hash TEXT NOT NULL DEFAULT '')");
            // Migration: thêm cột bcrypt_hash nếu DB cũ chưa có
            try {
                statement.execute("ALTER TABLE users ADD COLUMN bcrypt_hash TEXT NOT NULL DEFAULT ''");
            } catch (SQLException ignored) {
                // Cột đã tồn tại — bỏ qua
            }
            statement.execute("CREATE TABLE IF NOT EXISTS messages ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "sender TEXT NOT NULL, "
                    + "type TEXT NOT NULL, "
                    + "content TEXT NOT NULL, "
                    + "sent_at INTEGER NOT NULL)");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to initialize user database", ex);
        }
    }

    /** SHA-256 — chỉ dùng để verify tài khoản cũ trong quá trình lazy migration */
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
