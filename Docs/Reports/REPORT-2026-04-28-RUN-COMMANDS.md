# Lệnh build, test và chạy dự án

## 1. Build bằng Maven
```powershell
& "D:\Tools\apache-maven-3.9.9\bin\mvn.cmd" clean package
```
Kết quả: `target\ltm2-chat-1.0.0-all.jar` (fat jar bao gồm SQLite driver)

## 2. Chạy Server từ fat jar
```powershell
java -jar target\ltm2-chat-1.0.0-all.jar
```

## 3. Chạy Client từ fat jar
```powershell
java -cp target\ltm2-chat-1.0.0-all.jar client.ClientLauncher
```
> Mở cửa sổ riêng cho Server và Client. Có thể mở nhiều Client cùng lúc.

## 4. Chạy Server từ bản portable
```powershell
"dist\LTM2-Server\LTM2-Server.exe"
```

## 5. Chạy Client từ bản portable
```powershell
"dist\LTM2-Client\LTM2-Client.exe"
```

## 6. Luồng sử dụng chuẩn (v4 — UI tách Login/Chat)
1. Khởi động Server → nhấn **Start**
2. Khởi động Client → cửa sổ **LoginFrame** mở ra (giao diện FlatLaf)
3. Nhập host `127.0.0.1` và port `8080` → nhấn **Connect**
   - Nếu thành công: phần Account được kích hoạt, trạng thái hiện "Connected — please log in or register"
4. Đăng ký tài khoản mới (**Register**) hoặc đăng nhập (**Login**)
   - Đăng nhập thành công → **LoginFrame** đóng, **ChatFrame** mở với username ở header
5. Gửi text (Enter hoặc nút **Send**) hoặc file (nút **Attach**)
6. Nhấn **Logout** (hoặc đóng cửa sổ) → **ChatFrame** đóng, **LoginFrame** mở lại

## 7. Kiểm thử thủ công đề xuất
- Mở Server trước, mở 2 hoặc nhiều Client
- Đăng ký tài khoản mới từ LoginFrame
- Đăng nhập → xác nhận ChatFrame mở đúng username
- Gửi text ngắn, text dài (Enter hoặc nút Send)
- Gửi ảnh hợp lệ qua nút Attach
- Gửi file quá lớn (>10 MB) để kiểm tra chặn lỗi
- Nhấn Logout → xác nhận LoginFrame mở lại (không thoát app)
- Đóng ChatFrame bằng nút X → xác nhận LoginFrame mở lại

## 8. Ghi chú
- Các lệnh giả định bạn đang đứng ở thư mục gốc dự án `D:\Code\Java\LTM2`
- Không dùng `-cp target\classes` vì thiếu SQLite và FlatLaf dependency — phải dùng fat jar
- FlatLaf 3.4 được đóng gói vào fat jar, không cần cài thêm
