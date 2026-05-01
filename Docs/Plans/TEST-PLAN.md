# Kế hoạch kiểm thử thủ công

## Mục tiêu
- Xác nhận Server/Client chạy được sau khi build
- Xác nhận chức năng chat, đăng ký, đăng nhập, gửi ảnh và logout
- Xác nhận các ràng buộc bảo mật cơ bản đang hoạt động

## Phạm vi kiểm thử
### 1. Khởi chạy
- Mở Server bằng bản portable
- Mở Client bằng bản portable
- Mở nhiều Client cùng lúc

### 2. Tài khoản
- Đăng ký tài khoản mới
- Đăng ký trùng username
- Đăng nhập bằng tài khoản đúng
- Đăng nhập sai mật khẩu
- Logout rồi login lại

### 3. Chat
- Gửi text ngắn
- Gửi text dài gần giới hạn
- Gửi nhiều message liên tiếp
- Kiểm tra log hiển thị đúng định dạng

### 4. File ảnh
- Gửi ảnh hợp lệ
- Gửi file không phải ảnh
- Gửi file vượt giới hạn dung lượng
- Gửi filename có ký tự nguy hiểm

### 5. Bảo mật / ổn định
- Mở nhiều client đồng thời
- Thử spam request login
- Thử gửi message cực dài
- Thử path traversal qua tên file
- Đảm bảo server không treo khi client thoát đột ngột

## Tiêu chí pass
- Không crash
- Không treo GUI
- Không lưu file ngoài thư mục cho phép
- Không lộ password trong log
- Không nhận dữ liệu vượt giới hạn

## Ghi chú
- Kế hoạch này lưu trong `Docs/Plans`
- Kết quả kiểm thử thực tế sẽ ghi vào `Docs/Reports`
