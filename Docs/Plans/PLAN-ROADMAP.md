# Kế hoạch phát triển tiếp theo

## 1. Mục tiêu tối ưu hóa hệ thống chat
### Mục tiêu
- Giảm độ trễ khi gửi/nhận tin nhắn
- Tăng độ ổn định khi nhiều client kết nối đồng thời
- Giảm rủi ro treo luồng hoặc rò rỉ tài nguyên
- Chuẩn hóa giao thức để dễ mở rộng về sau

### Hướng triển khai
- Tách rõ phần xử lý kết nối, xử lý thông điệp và lưu trữ file
- Chuẩn hóa message format cho text, ảnh và phản hồi hệ thống
- Thêm timeout, giới hạn kích thước dữ liệu và cơ chế đóng kết nối an toàn
- Theo dõi trạng thái client để dễ đoán lỗi và phục hồi
- Tối ưu logging để không ảnh hưởng hiệu năng GUI và network loop

### Kết quả mong đợi
- Server phản hồi ổn định khi có nhiều client
- Giao thức dễ bảo trì và ít phụ thuộc vào UI
- Giảm lỗi đồng bộ và giảm xung đột khi ghi dữ liệu

## 2. Mục tiêu thêm tài khoản và đăng nhập cho client
### Mục tiêu
- Cho phép tạo tài khoản
- Cho phép đăng nhập trước khi sử dụng chat
- Có nền tảng để phân quyền và lưu lịch sử sau này

### Hướng triển khai
- Thiết kế mô hình dữ liệu người dùng: username, password hash, trạng thái tài khoản
- Tạo flow đăng ký, đăng nhập, đăng xuất
- Xác thực ở phía server, không tin dữ liệu client gửi lên
- Lưu thông tin người dùng bằng kho dữ liệu phù hợp
- Thiết kế GUI client có màn hình login/registration riêng

### Kết quả mong đợi
- Client chỉ được tham gia chat sau khi xác thực thành công
- Có thể mở rộng sang phân quyền hoặc hồ sơ người dùng
- Tách rõ giữa đăng nhập và phiên chat

## 3. Mục tiêu phân tích lỗ hổng bảo mật
### Mục tiêu
- Xác định các điểm dễ bị khai thác trong hệ thống hiện tại
- Đánh giá rủi ro theo các kiểu tấn công mạng phổ biến
- Ưu tiên các biện pháp giảm thiểu trước khi mở rộng tính năng

### Các nhóm rủi ro cần xem xét
- DoS / resource exhaustion qua kết nối hoặc dữ liệu lớn
- Spoofing / impersonation do thiếu xác thực mạnh
- Tampering do dữ liệu không được kiểm tra chặt chẽ
- Information disclosure do log hoặc phản hồi quá chi tiết
- Path traversal / file overwrite khi xử lý tên file
- Replay / session hijacking nếu sau này có login nhưng không quản lý phiên tốt

### Hướng triển khai
- Rà soát giao thức hiện tại và điểm nhập từ client
- Tìm nơi cần kiểm tra dữ liệu, giới hạn tần suất và xác thực
- Đề xuất biện pháp phòng vệ tương ứng cho từng rủi ro
- Tách phần phân tích rủi ro thành tài liệu riêng để dễ theo dõi

### Kết quả mong đợi
- Danh sách lỗ hổng ưu tiên theo mức độ nghiêm trọng
- Có đề xuất mitigation rõ ràng cho từng nhóm tấn công
- Dùng làm đầu vào cho các bước tối ưu hóa và thêm đăng nhập

## 4. Thứ tự ưu tiên đề xuất
1. Tối ưu hóa hệ thống chat
2. Bổ sung tạo tài khoản + đăng nhập
3. Phân tích và vá các lỗ hổng bảo mật

## 5. Ghi chú
- Kế hoạch này thuộc thư mục `Docs/Plans`
- Báo cáo triển khai sẽ lưu trong `Docs/Reports`
- Review chỉ tạo khi được yêu cầu
