# Roadmap phát triển và tăng cường bảo mật

## Giai đoạn 1. Ổn định nền tảng chat
### Mục tiêu
- Giảm độ trễ gửi/nhận
- Tăng khả năng chịu tải khi nhiều client kết nối đồng thời
- Làm rõ ranh giới giữa GUI, protocol, network và storage

### Việc cần làm
- Tách riêng luồng chấp nhận kết nối và luồng xử lý từng client
- Chuẩn hóa handler cho text, ảnh và phản hồi hệ thống
- Thêm timeout hợp lý cho socket và giới hạn kích thước payload
- Tối ưu logging để không chặn giao diện
- Bổ sung đóng tài nguyên an toàn khi lỗi xảy ra

### Kết quả mong đợi
- Server xử lý ổn định hơn dưới tải nhiều kết nối
- Mã nguồn dễ đọc, dễ mở rộng và dễ bảo trì

## Giai đoạn 2. Tạo tài khoản và đăng nhập
### Mục tiêu
- Cho phép người dùng đăng ký và đăng nhập trước khi chat
- Tạo nền tảng cho phân quyền và theo dõi phiên làm việc

### Việc cần làm
- Thiết kế mô hình dữ liệu user: username, password hash, salt, trạng thái
- Xây dựng luồng đăng ký, xác thực và đăng xuất
- Tách màn hình login/registration khỏi màn hình chat
- Xác thực ở phía server và chỉ cấp quyền chat sau khi login thành công
- Thiết kế cơ chế session an toàn cho client

### Kết quả mong đợi
- Client phải đăng nhập trước khi sử dụng chức năng chat
- Có thể mở rộng sang role-based access control sau này

## Giai đoạn 3. Vá lỗ hổng và tăng cường bảo mật mạng
### Mục tiêu
- Giảm khả năng bị khai thác bởi các kiểu tấn công mạng phổ biến
- Bịt các điểm yếu ở input, session, file handling và network handling

### Việc cần làm
- Rà soát tất cả điểm nhập dữ liệu từ client
- Áp dụng whitelist validation cho lệnh, tên file, kích thước và định dạng
- Thêm rate limiting theo IP hoặc theo phiên
- Chống DoS/resource exhaustion bằng giới hạn kết nối, giới hạn tốc độ và ngưỡng dữ liệu
- Chống path traversal bằng chuẩn hóa đường dẫn và loại bỏ ký tự nguy hiểm
- Giảm lộ thông tin trong log và phản hồi lỗi
- Thiết kế session token ngắn hạn và cơ chế hết hạn
- Xem xét mã hóa kênh truyền nếu nâng cấp ra môi trường ngoài localhost

### Kết quả mong đợi
- Danh sách lỗ hổng được xử lý theo mức độ ưu tiên
- Hệ thống ít bị lạm dụng hơn trước các tấn công phổ biến
- Có tài liệu rõ ràng để kiểm thử và kiểm toán sau này

## Giai đoạn 4. Kiểm thử và xác nhận
### Mục tiêu
- Xác nhận các thay đổi không làm vỡ luồng chat hiện tại
- Đánh giá hiệu quả của biện pháp bảo mật

### Việc cần làm
- Kiểm thử nhiều client đồng thời
- Kiểm thử đăng ký/đăng nhập với dữ liệu hợp lệ và không hợp lệ
- Kiểm thử các tình huống tấn công cơ bản như gửi dữ liệu lớn, tên file xấu, spam request
- Ghi nhận kết quả vào `Docs/Reports`

### Kết quả mong đợi
- Có số liệu và ghi chú kiểm thử rõ ràng
- Sẵn sàng cho bước tối ưu hóa tiếp theo hoặc mở rộng tính năng

## Thứ tự ưu tiên
1. Giai đoạn 1 - Ổn định nền tảng chat
2. Giai đoạn 3 - Vá lỗ hổng và tăng cường bảo mật mạng
3. Giai đoạn 2 - Tạo tài khoản và đăng nhập
4. Giai đoạn 4 - Kiểm thử và xác nhận

## Ghi chú
- Tài liệu này thuộc `Docs/Plans`
- Báo cáo từng giai đoạn lưu trong `Docs/Reports`
- Review chỉ tạo khi được yêu cầu
