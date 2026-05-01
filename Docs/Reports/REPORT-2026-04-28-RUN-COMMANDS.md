# Lệnh build, test và chạy dự án

## 1. Build bằng Maven
```powershell
& "D:\Tools\apache-maven-3.9.9\bin\mvn.cmd" clean package
```

## 2. Chạy Server từ source
```powershell
java -cp "target\classes" server.ServerApp
```

## 3. Chạy Client từ source
```powershell
java -cp "target\classes" client.ClientApp
```

## 4. Chạy Server từ bản portable
```powershell
"dist\LTM2-Server\LTM2-Server.exe"
```

## 5. Chạy Client từ bản portable
```powershell
"dist\LTM2-Client\LTM2-Client.exe"
```

## 6. Kiểm thử thủ công đề xuất
- Mở Server trước
- Mở 2 hoặc nhiều Client
- Đăng ký tài khoản mới
- Đăng nhập tài khoản
- Gửi text
- Gửi ảnh nhỏ hợp lệ
- Logout rồi đăng nhập lại
- Thử file quá lớn và tên file không hợp lệ để kiểm tra chặn lỗi

## 7. Ghi chú
- Review chỉ tạo khi được yêu cầu
- Các lệnh trên giả định bạn đang đứng ở thư mục gốc dự án `D:\Code\Java\LTM2`
