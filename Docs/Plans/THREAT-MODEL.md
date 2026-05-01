# Kế hoạch phân tích lỗ hổng và threat model

## 1. Mục tiêu
- Xác định các tài sản cần bảo vệ: tài khoản, mật khẩu, file ảnh, phiên làm việc, socket và dữ liệu chat
- Chỉ ra các bề mặt tấn công quan trọng trong client, server và giao thức
- Ưu tiên biện pháp giảm thiểu theo tác động và mức độ dễ khai thác

## 2. Bề mặt tấn công chính
### 2.1 Kết nối mạng
- Kết nối TCP trực tiếp tới server
- Không có lớp mã hóa nếu chạy ngoài localhost
- Có thể bị spam kết nối hoặc giữ kết nối lâu để tiêu tốn tài nguyên

### 2.2 Giao thức chat
- Các lệnh `TEXT`, `IMAGE`, `REGISTER`, `LOGIN`, `LOGOUT`, `DISCONNECT`
- Dữ liệu đầu vào hoàn toàn do client gửi lên
- Có nguy cơ gửi lệnh sai định dạng hoặc payload bất thường

### 2.3 Xử lý file
- Tên file ảnh do client cung cấp
- Kích thước file có thể lớn
- Có nguy cơ path traversal, ghi đè hoặc làm đầy ổ đĩa

### 2.4 Xác thực và phiên
- Tài khoản lưu trong DB cục bộ
- Có nguy cơ brute force đăng nhập
- Có nguy cơ tái sử dụng phiên nếu sau này thêm session token mà không quản lý chặt

## 3. Nhóm tấn công cần phân tích
### 3.1 DoS / Resource Exhaustion
- Flood kết nối
- Gửi dữ liệu lớn liên tục
- Giữ socket mở nhưng không gửi dữ liệu

### 3.2 Spoofing / Impersonation
- Giả mạo username
- Cố gắng đăng nhập bằng thông tin đoán được
- Tái sử dụng thông điệp hợp lệ từ phiên khác

### 3.3 Tampering
- Chỉnh sửa message hoặc file trong quá trình truyền
- Gửi dữ liệu sai kiểu để phá parser

### 3.4 Information Disclosure
- Log lộ thông tin nhạy cảm
- Phản hồi lỗi tiết lộ cấu trúc nội bộ
- Lưu mật khẩu không an toàn

### 3.5 Path Traversal / File Abuse
- Filename chứa `..`, ký tự đặc biệt hoặc đường dẫn tuyệt đối
- Tạo file ngoài thư mục cho phép

### 3.6 Brute Force / Credential Stuffing
- Thử mật khẩu nhiều lần
- Tấn công vào điểm login nếu thiếu giới hạn số lần

## 4. Biện pháp giảm thiểu
### 4.1 Input validation
- Whitelist command hợp lệ
- Giới hạn độ dài username, password, message, filename
- Loại bỏ ký tự nguy hiểm trong filename

### 4.2 Kết nối và phiên
- Timeout cho socket
- Giới hạn số client đồng thời
- Giới hạn số request/phút theo client/IP
- Chuẩn bị token/session nếu mở rộng auth

### 4.3 Xử lý file an toàn
- Dùng tên file sạch sau khi chuẩn hóa
- Không cho ghi ra ngoài thư mục lưu ảnh
- Giới hạn dung lượng và định dạng file

### 4.4 Bảo mật tài khoản
- Hash mật khẩu với salt
- Không lưu plaintext
- Giới hạn số lần login sai
- Thêm cơ chế khóa tạm thời nếu brute force

### 4.5 Logging an toàn
- Không log password, token hoặc dữ liệu nhạy cảm
- Log ngắn gọn, có cấu trúc
- Tách log kỹ thuật và log người dùng

## 5. Thứ tự ưu tiên vá lỗi
1. Input validation cho command và file
2. Bảo vệ login/account
3. Chống DoS và resource exhaustion
4. Chống path traversal và file abuse
5. Bảo mật log và phản hồi lỗi

## 6. Tiêu chí hoàn thành
- Không có điểm nhập quan trọng nào chưa được kiểm tra
- Login có giới hạn và dữ liệu tài khoản được bảo vệ tốt hơn
- Xử lý file an toàn trước path traversal
- Server chịu tải và lỗi tốt hơn trước các input bất thường

## 7. Ghi chú
- Tài liệu này thuộc `Docs/Plans`
- Báo cáo kết quả vá lỗi lưu trong `Docs/Reports`
- Review chỉ tạo khi được yêu cầu
