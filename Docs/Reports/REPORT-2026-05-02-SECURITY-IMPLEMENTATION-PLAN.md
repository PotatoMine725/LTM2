# Kế hoạch Triển khai Cải tiến Bảo mật — LTM2 Chat
**Ngày:** 2026-05-02 | **Phiên bản:** 1.0 | **Dựa trên:** Audit code thực tế toàn bộ src/

---

## 1. Tổng quan lỗ hổng đã phát hiện

| ID | Lỗ hổng | File / Dòng | Mức độ | Sprint |
|----|---------|-------------|--------|--------|
| C1 | Mật khẩu plaintext qua TCP | `ChatClient.java:189`, `ClientHandler.java:79` | 🔴 CRITICAL | S1 |
| C2 | SHA-256 không phù hợp hash password | `UserStore.java:27–29` | 🔴 CRITICAL | S1 |
| C3 | Salt tất định (dựa vào username) | `UserStore.java:28` | 🔴 CRITICAL | S1 |
| H1 | Không rate-limit đăng nhập | `ClientHandler.java:78–92` | 🟠 HIGH | S2 |
| H2 | TEXT/IMAGE không yêu cầu xác thực | `ClientHandler.java:49–54` | 🟠 HIGH | S2 |
| H3 | Không kiểm tra loại file thực sự | `ClientHandler.java:131–165` | 🟠 HIGH | S2 |
| M1 | Không giới hạn độ dài username/password | `UserStore.java:22–25` | 🟡 MEDIUM | S3 |
| M2 | File ảnh trùng tên bị ghi đè | `ClientHandler.java:145` | 🟡 MEDIUM | S3 |
| M3 | Port parse không có try-catch (Server UI) | `ServerFrame.java:68` | 🟡 MEDIUM | S3 |
| M4 | DB file không được bảo vệ bởi OS | `UserStore.java:16` | 🟡 MEDIUM | S3 |
| L1 | Timing attack enumerate username | `UserStore.java:43–62` | 🔵 LOW | S3 |
| L2 | Không giới hạn số kết nối đồng thời | `ChatServer.java` | 🔵 LOW | S3 |
| L3 | Socket timeout quá dài (5 phút) | `ClientHandler.java:38` | 🔵 LOW | S3 |

---

## 2. Phân tích chi tiết và hướng sửa

### 🔴 C1 — Mật khẩu plaintext qua TCP

**Root cause:** `Socket` TCP thông thường, không mã hóa.

```java
// ChatClient.java:189 — hiện tại
output.writeUTF(username.trim());
output.writeUTF(password.trim()); // plaintext!
```

**Fix — Bọc socket bằng TLS (SSLSocket):**

```java
// Server — ChatServer.java
SSLServerSocketFactory factory =
    (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
serverSocket = (SSLServerSocket) factory.createServerSocket(port);

// Client — ChatClient.java
SSLSocketFactory factory =
    (SSLSocketFactory) SSLSocketFactory.getDefault();
socket = (SSLSocket) factory.createSocket(host, port);
```

**Cần thêm:** Tạo self-signed cert bằng `keytool`, bundle keystore vào resources.

---

### 🔴 C2 + C3 — SHA-256 + Salt tất định

**Root cause:** SHA-256 quá nhanh (GPU thử tỷ hash/giây). Salt = `hash(username + ":salt")` — có thể tính trước.

```java
// UserStore.java:27–29 — hiện tại
String passwordHash = hash(password.trim());
String salt = hash(trimmedUsername + ":salt"); // KHÔNG ngẫu nhiên
String storedPassword = hash(passwordHash + salt);
```

**Fix — Dùng BCrypt (tích hợp random salt, cost factor):**

```xml
<!-- pom.xml — thêm dependency -->
<dependency>
    <groupId>at.favre.lib</groupId>
    <artifactId>bcrypt</artifactId>
    <version>0.10.2</version>
</dependency>
```

```java
// UserStore.java — register()
String bcryptHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
// Lưu bcryptHash vào cột password_hash. Bỏ cột salt.

// UserStore.java — authenticate()
BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), storedHash);
return result.verified;
```

**Thay đổi schema:** Xóa cột `salt` (BCrypt tự nhúng salt vào hash string).

---

### 🟠 H1 — Không rate-limit đăng nhập

**Root cause:** Giao thức cho phép gửi `LOGIN` vô số lần trong cùng connection.

**Fix — Thêm bộ đếm trong `ClientHandler`:**

```java
private int loginFailures = 0;
private static final int MAX_LOGIN_ATTEMPTS = 5;

private void handleLogin() throws IOException {
    if (loginFailures >= MAX_LOGIN_ATTEMPTS) {
        output.writeUTF(AccountProtocol.AUTH_FAIL);
        output.writeUTF("Too many failed attempts");
        output.flush();
        active = false; // ngắt kết nối
        return;
    }
    String user = sanitize(input.readUTF());
    String pass = input.readUTF();
    boolean ok = userStore != null && userStore.authenticate(user, pass);
    if (!ok) loginFailures++;
    // ... phần còn lại giữ nguyên
}
```

---

### 🟠 H2 — TEXT/IMAGE không yêu cầu xác thực

**Root cause:** `run()` không kiểm tra `username != null` trước khi gọi `handleText()`/`handleImage()`.

**Fix — Guard trong vòng lặp `run()`:**

```java
} else if (Protocol.TEXT.equals(command)) {
    if (username == null) {
        logEvent("Unauthenticated TEXT rejected from " + socket.getRemoteSocketAddress());
    } else {
        handleText();
    }
} else if (Protocol.IMAGE.equals(command)) {
    if (username == null) {
        logEvent("Unauthenticated IMAGE rejected");
    } else {
        handleImage();
    }
}
```

---

### 🟠 H3 — Không kiểm tra magic bytes file

**Root cause:** Chỉ kiểm tra tên file, không xác minh nội dung thực.

**Fix — Kiểm tra magic bytes sau khi nhận xong:**

```java
private boolean isValidImageBytes(File file) throws IOException {
    byte[] header = new byte[4];
    try (FileInputStream fis = new FileInputStream(file)) {
        if (fis.read(header) < 4) return false;
    }
    // JPEG: FF D8 FF
    if (header[0] == (byte)0xFF && header[1] == (byte)0xD8) return true;
    // PNG: 89 50 4E 47
    if (header[0] == (byte)0x89 && header[1] == 0x50) return true;
    // GIF: 47 49 46 38
    if (header[0] == 0x47 && header[1] == 0x49) return true;
    return false;
}

// Trong handleImage(), sau khi ghi xong file:
if (!isValidImageBytes(target)) {
    target.delete();
    throw new IOException("Not a valid image file");
}
```

---

### 🟡 M1 — Không giới hạn độ dài input

**Fix — Trong `UserStore.register()`:**

```java
if (username.length() < 3 || username.length() > 32) return false;
if (password.length() < 8 || password.length() > 128) return false;
if (!username.matches("[a-zA-Z0-9_]+")) return false;
```

---

### 🟡 M2 — File ảnh trùng tên bị ghi đè

**Fix — Thêm timestamp prefix:**

```java
// ClientHandler.java:145
String uniqueName = System.currentTimeMillis() + "_" + safeName;
File target = new File(dir, uniqueName);
```

---

### 🟡 M3 — Port parse crash trên Server UI

**Fix — `ServerFrame.java`:**

```java
try {
    int port = Integer.parseInt(portField.getText().trim());
    if (port < 1 || port > 65535) throw new NumberFormatException();
    server.start(port);
} catch (NumberFormatException ex) {
    statusLabel.setText("Invalid port (1–65535)");
}
```

---

### 🟡 M4 — DB file không được bảo vệ

**Fix — Đặt DB trong thư mục user-data với quyền OS:**

```java
// UserStore.java
private static final String DB_URL;
static {
    String dataDir = System.getProperty("user.home") + "/.ltm2";
    new File(dataDir).mkdirs();
    DB_URL = "jdbc:sqlite:" + dataDir + "/ltm2-users.db";
}
```

---

### 🔵 L1 — Timing attack

**Fix — Luôn tính hash dù user không tồn tại:**

```java
if (!resultSet.next()) {
    hash(hash(password.trim()) + "dummy_constant_salt_placeholder");
    return false;
}
```

---

### 🔵 L2 — Không giới hạn kết nối

**Fix — Semaphore trong `ChatServer`:**

```java
private final Semaphore connectionLimit = new Semaphore(100);

// acceptLoop — trước khi start worker:
if (!connectionLimit.tryAcquire()) {
    socket.close();
    logEvent("Connection limit reached, client rejected");
    continue;
}
// onClose callback:
() -> { connectionLimit.release(); workers.remove(Thread.currentThread()); }
```

---

### 🔵 L3 — Socket timeout quá dài

**Fix — `ClientHandler.java:38`:**

```java
socket.setSoTimeout(2 * 60 * 1000); // 2 phút trước khi xác thực
// Sau login thành công trong handleLogin():
socket.setSoTimeout(30 * 60 * 1000); // 30 phút sau xác thực
```

---

## 3. Lộ trình triển khai (3 Sprint)

### Sprint 1 — Critical (Tuần 1) — Bảo vệ dữ liệu người dùng

**Mục tiêu:** Không còn dữ liệu nhạy cảm đi qua mạng dưới dạng plaintext.

| Task | File thay đổi | Effort |
|------|--------------|--------|
| Tạo self-signed keystore bằng `keytool` | `src/main/resources/server.keystore` | 1h |
| Thay `Socket` → `SSLSocket` phía Server | `ChatServer.java` | 2h |
| Thay `Socket` → `SSLSocket` phía Client | `ChatClient.java` | 2h |
| Thêm dependency BCrypt vào pom.xml | `pom.xml` | 0.5h |
| Migrate `UserStore.register()` sang BCrypt | `UserStore.java` | 1h |
| Migrate `UserStore.authenticate()` sang BCrypt | `UserStore.java` | 1h |
| Migration script: hash lại passwords cũ trong DB | Script SQL | 1h |
| Xóa cột `salt` khỏi schema (ALTER TABLE) | `UserStore.initDatabase()` | 0.5h |

**Tiêu chí hoàn thành Sprint 1:**
- [ ] Wireshark capture không thấy mật khẩu plaintext
- [ ] Đăng nhập với tài khoản cũ vẫn hoạt động sau migration
- [ ] BCrypt hash có prefix `$2a$12$` trong DB

---

### Sprint 2 — High (Tuần 2) — Ngăn chặn tấn công chủ động

| Task | File thay đổi | Effort |
|------|--------------|--------|
| Rate limit đăng nhập (5 lần / connection) | `ClientHandler.java` | 1h |
| Guard xác thực trước TEXT/IMAGE | `ClientHandler.java` | 0.5h |
| Kiểm tra magic bytes sau upload ảnh | `ClientHandler.java` | 2h |
| Test: gửi TEXT không đăng nhập → bị reject | Manual test | 0.5h |
| Test: upload .exe → bị xóa và báo lỗi | Manual test | 0.5h |

**Tiêu chí hoàn thành Sprint 2:**
- [ ] Sau 5 lần login sai, kết nối bị đóng
- [ ] Gửi `TEXT` mà không login → server log "Unauthenticated rejected"
- [ ] Upload file .exe → server xóa và trả lỗi

---

### Sprint 3 — Medium/Low (Tuần 3) — Hardening tổng thể

| Task | File thay đổi | Effort |
|------|--------------|--------|
| Validate độ dài + regex username/password | `UserStore.java` | 1h |
| Unique filename (timestamp prefix) | `ClientHandler.java` | 0.5h |
| Port parse với try-catch trên ServerFrame | `ServerFrame.java` | 0.5h |
| Di chuyển DB vào `~/.ltm2/` | `UserStore.java` | 1h |
| Constant-time auth (dummy hash) | `UserStore.java` | 0.5h |
| Semaphore giới hạn 100 kết nối | `ChatServer.java` | 1h |
| Timeout 2 phút pre-auth / 30 phút post-auth | `ClientHandler.java` | 0.5h |

**Tiêu chí hoàn thành Sprint 3:**
- [ ] Username < 3 hoặc > 32 ký tự → register thất bại
- [ ] Hai file `photo.jpg` không ghi đè nhau
- [ ] Nhập port "abc" → hiện thông báo lỗi, không crash
- [ ] DB nằm trong `~/.ltm2/`, không phải working directory

---

## 4. Ma trận rủi ro sau khi sửa

| Lỗ hổng | Trước | Sau Sprint 1 | Sau Sprint 2 | Sau Sprint 3 |
|---------|-------|-------------|-------------|-------------|
| Plaintext password | 🔴 | ✅ Đã vá | ✅ | ✅ |
| Weak password hash | 🔴 | ✅ Đã vá | ✅ | ✅ |
| Deterministic salt | 🔴 | ✅ (BCrypt) | ✅ | ✅ |
| Brute-force login | 🟠 | 🟠 | ✅ Đã vá | ✅ |
| Unauthenticated send | 🟠 | 🟠 | ✅ Đã vá | ✅ |
| File type bypass | 🟠 | 🟠 | ✅ Đã vá | ✅ |
| Input length DoS | 🟡 | 🟡 | 🟡 | ✅ Đã vá |
| File overwrite | 🟡 | 🟡 | 🟡 | ✅ Đã vá |
| Port parse crash | 🟡 | 🟡 | 🟡 | ✅ Đã vá |
| DB exposure | 🟡 | 🟡 | 🟡 | ✅ Đã vá |
| Timing attack | 🔵 | 🔵 | 🔵 | ✅ Đã vá |
| Connection flood | 🔵 | 🔵 | 🔵 | ✅ Đã vá |
| Idle connection DoS | 🔵 | 🔵 | 🔵 | ✅ Đã vá |

---

## 5. Phụ thuộc cần thêm vào pom.xml

```xml
<!-- BCrypt — thay thế SHA-256 cho password hashing -->
<dependency>
    <groupId>at.favre.lib</groupId>
    <artifactId>bcrypt</artifactId>
    <version>0.10.2</version>
</dependency>
```

TLS/SSL sử dụng `javax.net.ssl` có sẵn trong JDK 17, không cần thêm dependency.

---

## 6. Lệnh tạo keystore (chạy một lần)

```powershell
keytool -genkeypair `
  -alias ltm2 `
  -keyalg RSA `
  -keysize 2048 `
  -validity 3650 `
  -keystore src/main/resources/server.keystore `
  -storepass ltm2pass `
  -dname "CN=localhost, OU=LTM2, O=LTM2, L=HCM, S=HCM, C=VN"
```

Keystore được bundle vào fat jar, client load bằng `TrustManager` accept self-signed hoặc qua trust store riêng.

---

## 7. Ghi chú

- Dự án hiện được đánh dấu "local-only" nhưng lộ trình bảo mật này áp dụng ngay từ giai đoạn học tập.
- Tài liệu audit chi tiết: `REPORT-2026-05-02-SECURITY.md`
- Kế hoạch bảo mật gốc: `Docs/Plans/SECURITY-PLAN.md`
- Threat model: `Docs/Plans/THREAT-MODEL.md`
