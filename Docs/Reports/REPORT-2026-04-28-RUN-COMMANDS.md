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

## 6. Luồng sử dụng chuẩn
1. Khởi động Server → nhấn **Start**
2. Khởi động Client → nhập host `127.0.0.1` và port `8080` → nhấn **Connect**
3. Đăng ký tài khoản mới (**Register**) hoặc đăng nhập (**Login**)
4. Gửi text hoặc ảnh
5. Logout / Disconnect khi xong (sau Logout phải Connect lại mới Login được)

## 7. Kiểm thử thủ công đề xuất
- Mở Server trước, mở 2 hoặc nhiều Client
- Đăng ký tài khoản mới
- Đăng nhập tài khoản
- Gửi text ngắn, text dài
- Gửi ảnh hợp lệ
- Gửi file quá lớn (>10 MB) và tên file có ký tự nguy hiểm để kiểm tra chặn lỗi
- Logout rồi Connect lại và Login lại

## 8. Ghi chú
- Các lệnh giả định bạn đang đứng ở thư mục gốc dự án `D:\Code\Java\LTM2`
- Không dùng `-cp target\classes` vì thiếu SQLite dependency — phải dùng fat jar
