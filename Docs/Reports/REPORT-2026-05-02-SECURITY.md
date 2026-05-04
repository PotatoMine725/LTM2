# Báo cáo Kiểm tra Bảo mật — LTM2 Chat — 2026-05-02

## Tóm tắt mức độ rủi ro

| Mức | Số lượng |
|-----|---------|
| 🔴 CRITICAL | 3 |
| 🟠 HIGH | 3 |
| 🟡 MEDIUM | 4 |
| 🔵 LOW | 3 |

---

## 🔴 CRITICAL

### C1 — Mật khẩu truyền qua mạng dưới dạng plaintext

**File:** `client/ChatClient.java:189`, `server/ClientHandler.java:79–80`

Kết nối dùng `Socket` TCP thông thường (không phải `SSLSocket`). Khi đăng nhập, client ghi:

```java
output.writeUTF(username.trim());
output.writeUTF(password.trim());   // <-- plaintext qua mạng
```

Bất kỳ ai có thể chặn gói tin trên mạng (Wireshark, ARP spoofing) đều đọc được mật khẩu nguyên văn.

**Phương án sửa:** Bọc socket bằng TLS — thay `Socket` bằng `SSLSocket`:

```java
// Server
SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port);

// Client
SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
```

Cần tạo self-signed certificate bằng `keytool` và phân phối keystore cùng app.

---

### C2 — SHA-256 không phù hợp để hash mật khẩu

**File:** `server/UserStore.java:27–29`, `UserStore.java:126–138`

```java
String passwordHash = hash(password.trim());        // SHA-256
String salt = hash(trimmedUsername + ":salt");      // deterministic, không ngẫu nhiên
String storedPassword = hash(passwordHash + salt);  // double SHA-256
```

**Vấn đề:**
- SHA-256 tính trong ~microseconds — GPU hiện đại có thể thử hàng tỷ hash/giây (brute-force offline).
- Salt hoàn toàn tất định: biết username → biết salt → có thể precompute rainbow table per-user.

**Phương án sửa:** Dùng **BCrypt** (hoặc Argon2id) — thiết kế chuyên cho password, cost factor tùy chỉnh được:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>at.favre.lib</groupId>
    <artifactId>bcrypt</artifactId>
    <version>0.10.2</version>
</dependency>
```

```java
// Register
String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

// Authenticate
BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), storedHash);
return result.verified;
```

BCrypt tự tạo random salt 16 bytes và nhúng vào chuỗi hash — không cần cột `salt` riêng.

---

### C3 — Salt tất định (không ngẫu nhiên)

**File:** `server/UserStore.java:28`

```java
String salt = hash(trimmedUsername + ":salt");
```

Salt phải là chuỗi **ngẫu nhiên**, không thể đoán, khác nhau cho mỗi user và mỗi lần đổi mật khẩu. Salt này dựa hoàn toàn vào username — hai người dùng có username giống nhau ở hệ thống khác sẽ có cùng salt.

**Phương án sửa (nếu chưa migrate sang BCrypt):**

```java
// Tạo random salt
SecureRandom random = new SecureRandom();
byte[] saltBytes = new byte[16];
random.nextBytes(saltBytes);
String salt = Base64.getEncoder().encodeToString(saltBytes);
```

---

## 🟠 HIGH

### H1 — Không có giới hạn số lần đăng nhập (Brute-force)

**File:** `server/ClientHandler.java:78–92`

Không có bất kỳ cơ chế nào ngăn attacker thử mật khẩu liên tục. Giao thức cho phép gửi `LOGIN` nhiều lần trong cùng một connection.

**Phương án sửa:**

```java
// ClientHandler — thêm bộ đếm thất bại
private int loginFailures = 0;
private static final int MAX_LOGIN_ATTEMPTS = 5;

private void handleLogin() throws IOException {
    if (loginFailures >= MAX_LOGIN_ATTEMPTS) {
        output.writeUTF(AccountProtocol.AUTH_FAIL);
        output.writeUTF("Too many attempts — disconnected");
        output.flush();
        active = false;
        return;
    }
    // ... logic hiện tại ...
    if (!ok) {
        loginFailures++;
    }
}
```

Nâng cao hơn: lưu IP + số lần thất bại vào `UserStore` để lockout theo IP sau N lần trong X phút.

---

### H2 — Gửi TEXT/IMAGE mà không cần đăng nhập

**File:** `server/ClientHandler.java:49–54`, `handleText():117–129`, `handleImage():131–165`

Giao thức không kiểm tra trạng thái xác thực trước khi xử lý `TEXT` hay `IMAGE`. Bất kỳ ai kết nối TCP đều có thể gửi ảnh và tin nhắn mà không đăng nhập.

```java
private void handleText() throws IOException {
    // Không có kiểm tra: if (username == null) reject
    String message = input.readUTF();
    ...
    messageConsumer.accept(prefix() + escape(message));  // vẫn được broadcast
}
```

**Phương án sửa:** Thêm guard ở đầu mỗi handler và trong vòng lặp chính:

```java
} else if (Protocol.TEXT.equals(command)) {
    if (username == null) {
        logEvent("Unauthenticated TEXT rejected");
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

### H3 — Không kiểm tra loại file khi upload ảnh

**File:** `server/ClientHandler.java:131–165`

Server chỉ kiểm tra tên file và kích thước, không xác minh file có thực sự là ảnh. Attacker có thể upload file `.exe`, `.bat`, `.html` hoặc bất kỳ loại nào.

**Phương án sửa:** Kiểm tra magic bytes:

```java
// Sau khi nhận xong file, kiểm tra magic bytes
private boolean isImage(File file) throws IOException {
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
```

Nếu không phải ảnh thì xóa file và từ chối.

---

## 🟡 MEDIUM

### M1 — Username/Password không có ràng buộc độ dài

**File:** `server/UserStore.java:22–25`, `client/LoginFrame.java:114–115`

Chỉ kiểm tra `isBlank()`. Không có giới hạn min/max:
- `"a"` là username/password hợp lệ
- Username 10.000 ký tự là hợp lệ (DoS tiềm năng)

**Phương án sửa:**

```java
boolean register(String username, String password) {
    if (username == null || username.length() < 3 || username.length() > 32) return false;
    if (password == null || password.length() < 8 || password.length() > 128) return false;
    if (!username.matches("[a-zA-Z0-9_]+")) return false;  // chỉ cho phép ký tự an toàn
    ...
}
```

---

### M2 — File ảnh trùng tên bị ghi đè (Overwrite)

**File:** `server/ClientHandler.java:145`

```java
File target = new File(dir, safeName);
try (FileOutputStream fileOut = new FileOutputStream(target)) {  // overwrite nếu tồn tại
```

User A upload `photo.jpg`, User B upload `photo.jpg` → file của A bị mất hoàn toàn.

**Phương án sửa:** Thêm prefix timestamp hoặc UUID:

```java
String uniqueName = System.currentTimeMillis() + "_" + safeName;
File target = new File(dir, uniqueName);
```

---

### M3 — Port parse không có try-catch trên Server UI

**File:** `server/ServerFrame.java:68`

```java
startButton.addActionListener(e -> {
    int port = Integer.parseInt(portField.getText().trim());  // NumberFormatException uncaught
    server.start(port);
});
```

Nhập sai port → app crash với exception chưa bắt.

**Phương án sửa:**

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

### M4 — Database file không được bảo vệ

**File:** `server/UserStore.java:16`

```java
private static final String DB_URL = "jdbc:sqlite:ltm2-users.db";
```

File `ltm2-users.db` nằm trong working directory — không có quyền hạn, không có mã hóa. Bất kỳ user nào truy cập vào máy chủ đều có thể copy/đọc toàn bộ hash mật khẩu.

**Phương án sửa:**
- Đặt DB trong thư mục riêng có quyền hạn OS (chỉ server process được đọc).
- Dùng SQLCipher để mã hóa DB nếu cần bảo mật cao hơn.
- Minimum: đặt đường dẫn tuyệt đối, không phụ thuộc working directory.

---

## 🔵 LOW

### L1 — Timing attack để enumerate username

**File:** `server/UserStore.java:43–62`

Khi username không tồn tại, hàm `authenticate()` trả về `false` ngay (không tính hash). Khi username tồn tại nhưng sai mật khẩu, phải tính SHA-256 trước khi trả về. Độ trễ khác nhau ~microseconds cho phép attacker biết username có tồn tại hay không qua timing.

**Phương án sửa:** Luôn tính hash dù user không tồn tại:

```java
if (!resultSet.next()) {
    hash(hash(password.trim()) + "dummy_salt");  // constant-time dummy
    return false;
}
```

---

### L2 — Không có giới hạn số kết nối đồng thời

Server chấp nhận không giới hạn kết nối. Attacker có thể mở 10.000 socket → cạn kiệt thread/FD của server (DoS).

**Phương án sửa:** Thêm `Semaphore` trong `ChatServer`:

```java
private final Semaphore connectionLimit = new Semaphore(100);

// Khi accept connection mới:
if (!connectionLimit.tryAcquire()) {
    socket.close();  // từ chối kết nối thứ 101
    return;
}
// Khi đóng connection:
connectionLimit.release();
```

---

### L3 — Socket timeout quá dài (5 phút)

**File:** `server/ClientHandler.java:38`

```java
socket.setSoTimeout(5 * 60 * 1000);  // 5 phút
```

5 phút cho phép attacker giữ nhiều kết nối idle để làm cạn tài nguyên, kết hợp với L2 thành DoS hiệu quả hơn.

**Phương án sửa:** Rút xuống 2 phút (120.000ms) và sau khi xác thực thành công có thể tăng lên lại:

```java
socket.setSoTimeout(2 * 60 * 1000);  // 2 phút trước login
// Sau khi login thành công:
socket.setSoTimeout(30 * 60 * 1000); // 30 phút sau login
```

---

## Lộ trình ưu tiên triển khai

```
Tuần 1 — Critical (bảo vệ dữ liệu người dùng)
  ├── C1: TLS/SSLSocket cho toàn bộ kết nối
  ├── C2: Migrate sang BCrypt
  └── C3: (tự động giải quyết khi dùng BCrypt)

Tuần 2 — High (ngăn chặn tấn công)
  ├── H1: Rate limiting + lockout sau 5 lần thất bại
  ├── H2: Guard xác thực trước TEXT/IMAGE
  └── H3: Kiểm tra magic bytes file upload

Tuần 3 — Medium/Low (hardening)
  ├── M1: Validate độ dài username/password
  ├── M2: Unique filename cho ảnh
  ├── M3: Port parse với try-catch
  ├── L1: Constant-time auth
  └── L2+L3: Connection limit + timeout tuning
```

---

## Tổng quan

| Lỗ hổng | File | Mức | Trạng thái |
|---------|------|-----|-----------|
| Plaintext password qua TCP | `ChatClient.java`, `ClientHandler.java` | 🔴 CRITICAL | Chưa sửa |
| SHA-256 + deterministic salt | `UserStore.java` | 🔴 CRITICAL | Chưa sửa |
| Không rate-limit đăng nhập | `ClientHandler.java` | 🟠 HIGH | Chưa sửa |
| Gửi TEXT/IMAGE không cần auth | `ClientHandler.java` | 🟠 HIGH | Chưa sửa |
| Không kiểm tra loại file | `ClientHandler.java` | 🟠 HIGH | Chưa sửa |
| Không giới hạn độ dài input | `UserStore.java` | 🟡 MEDIUM | Chưa sửa |
| File ảnh trùng tên bị ghi đè | `ClientHandler.java` | 🟡 MEDIUM | Chưa sửa |
| Port parse crash | `ServerFrame.java` | 🟡 MEDIUM | Chưa sửa |
| DB file không được bảo vệ | `UserStore.java` | 🟡 MEDIUM | Chưa sửa |
| Timing attack enumerate user | `UserStore.java` | 🔵 LOW | Chưa sửa |
| Không giới hạn số kết nối | `ChatServer.java` | 🔵 LOW | Chưa sửa |
| Socket timeout quá dài | `ClientHandler.java` | 🔵 LOW | Chưa sửa |
