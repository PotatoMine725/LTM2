# Kế hoạch vá lỗ hổng và tăng cường bảo mật mạng

## 1. Phạm vi bảo mật
- Bảo vệ kênh chat TCP nội bộ trên localhost
- Bảo vệ các luồng xử lý kết nối, gửi tin nhắn, gửi ảnh và đăng nhập trong tương lai
- Hạn chế rủi ro từ dữ liệu đầu vào không tin cậy

## 2. Các nhóm lỗ hổng cần vá
### 2.1 DoS / Resource Exhaustion
- Kết nối đồng loạt làm cạn luồng hoặc socket
- Payload lớn làm đầy bộ nhớ hoặc ổ đĩa
- Gửi request liên tục để làm nghẽn server

### 2.2 Spoofing / Impersonation
- Client giả mạo danh tính nếu chưa có xác thực
- Session bị mạo danh nếu cơ chế đăng nhập không chặt

### 2.3 Tampering / Data Manipulation
- Lệnh hoặc dữ liệu bị chỉnh sửa trên đường truyền
- File ảnh hoặc text có nội dung bất thường để phá luồng xử lý

### 2.4 Information Disclosure
- Log quá chi tiết làm lộ dữ liệu nhạy cảm
- Phản hồi lỗi chứa thông tin nội bộ của server

### 2.5 Path Traversal / File Overwrite
- Tên file ảnh có ký tự nguy hiểm
- Ghi đè file ngoài thư mục cho phép

### 2.6 Session Hijacking / Replay
- Token hoặc phiên làm việc không hết hạn
- Request cũ có thể bị gửi lại để tạo hành vi ngoài ý muốn

## 3. Biện pháp vá và tăng cường
### 3.1 Xác thực đầu vào
- Chỉ chấp nhận command hợp lệ theo whitelist
- Kiểm tra username, password, filename và kích thước file
- Loại bỏ ký tự nguy hiểm và chuẩn hóa chuỗi trước khi xử lý

### 3.2 Quản lý kết nối
- Giới hạn số client đồng thời
- Thêm timeout cho socket đọc/ghi
- Đóng tài nguyên ngay khi phát hiện bất thường

### 3.3 Chống lạm dụng
- Rate limiting theo IP/phiên
- Giới hạn số request trong một khoảng thời gian
- Tạm khóa hoặc từ chối client có hành vi bất thường

### 3.4 Bảo vệ file và storage
- Chỉ lưu trong thư mục được phép
- Dùng tên file an toàn sau khi chuẩn hóa
- Giới hạn dung lượng ảnh và loại file
- Không ghi đè file ngoài ý muốn

### 3.5 Bảo vệ session
- Dùng session token ngắn hạn khi có login
- Hết hạn token sau thời gian nhất định
- Thu hồi token khi logout hoặc disconnect

### 3.6 Giảm lộ thông tin
- Log ngắn gọn, không in dữ liệu nhạy cảm
- Thông báo lỗi chung chung cho client
- Tách log kỹ thuật khỏi log người dùng

### 3.7 Tăng cường kênh truyền
- Nếu vượt khỏi localhost, bật TLS cho TCP
- Xem xét cơ chế ký/kiểm tra toàn vẹn message

## 4. Thứ tự thực hiện
1. Vá input validation và file handling
2. Giới hạn kết nối, timeout và rate limiting
3. Thiết kế authentication/session an toàn
4. Tăng cường logging và bảo vệ thông tin
5. Đánh giá TLS và kiểm tra toàn vẹn dữ liệu

## 5. Tiêu chí hoàn thành
- Không còn chỗ nào tin tưởng dữ liệu client một cách tuyệt đối
- Server không bị treo hoặc cạn tài nguyên bởi input phổ biến
- Các lỗi bảo mật được ghi nhận, ưu tiên và xử lý theo mức độ

## 6. Ghi chú
- Tài liệu này thuộc `Docs/Plans`
- Báo cáo kết quả vá lỗi lưu trong `Docs/Reports`
- Review chỉ tạo khi được yêu cầu
