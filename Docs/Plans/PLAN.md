# Kế hoạch xây dựng TCP Chat Client-Server đa luồng

## Mục tiêu
Xây dựng ứng dụng chat TCP bằng Java chạy local trên `127.0.0.1` theo yêu cầu trong `README.md`, bao gồm:
- GUI cho Server và Client
- Gửi/nhận tin nhắn text
- Gửi ảnh từ Client sang Server
- Server đa luồng, cho phép nhiều client kết nối đồng thời
- Lưu ảnh nhận được và ghi log trạng thái

## Quy ước lưu tài liệu
- Kế hoạch lưu trong `Docs/Plans`
- Báo cáo lưu trong `Docs/Reports`
- Reviews lưu trong `Docs/Reviews`
- Chỉ thực hiện review khi được yêu cầu

## Phạm vi thực hiện
### Server
- Khởi động/dừng server bằng GUI
- Lắng nghe kết nối tại một port cấu hình
- Mỗi client được xử lý trên một luồng riêng để hỗ trợ đồng thời nhiều kết nối
- Hiển thị trạng thái, log, và tin nhắn nhận được
- Nhận `TEXT`, `IMAGE`, `DISCONNECT`
- Lưu file ảnh hợp lệ vào thư mục `received_images/`

### Client
- Kết nối tới `127.0.0.1`
- Nhập và gửi chat text
- Chọn file ảnh để gửi
- Ngắt kết nối an toàn

## Thiết kế kỹ thuật
### Gói `shared`
- `Protocol`: hằng số giao thức, tên lệnh, cấu hình giới hạn

### Gói `server`
- `ServerApp`: điểm khởi chạy server
- `ServerFrame`: GUI server bằng Swing
- `ChatServer`: chấp nhận nhiều socket client và điều phối luồng xử lý
- `ClientHandler`: xử lý riêng từng client trên một luồng
- `FileStorage`: lưu ảnh nhận được an toàn
- `ServerBroadcaster` hoặc cơ chế tương đương: phát thông báo trạng thái tới GUI

### Gói `client`
- `ClientApp`: điểm khởi chạy client
- `ClientFrame`: GUI client bằng Swing
- `ChatClient`: xử lý kết nối và gửi dữ liệu
- `ImageSender`: đọc file và gửi ảnh theo giao thức

## Giao thức truyền thông
- `TEXT <nội dung>`: gửi tin nhắn văn bản
- `IMAGE <filename> <filesize>` + dữ liệu nhị phân: gửi ảnh
- `DISCONNECT`: đóng kết nối

## Các bước triển khai
1. Tạo cấu trúc thư mục Java và các package
2. Cài đặt lớp giao thức dùng chung
3. Thiết kế server đa luồng với cơ chế nhận nhiều client đồng thời
4. Xây dựng luồng xử lý riêng cho từng client
5. Xây dựng GUI server
6. Xây dựng client socket
7. Xây dựng GUI client
8. Tích hợp gửi ảnh và lưu ảnh
9. Kiểm tra nhiều client kết nối đồng thời, gửi tin nhắn, ngắt kết nối và tái kết nối
10. Bổ sung comment cho các đoạn mã quan trọng

## Kiểm thử dự kiến
- Start/Stop server
- Kết nối nhiều client tới server cùng lúc
- Gửi nhiều tin nhắn text từ nhiều client
- Gửi ảnh hợp lệ từ nhiều client
- Thử gửi file không phải ảnh
- Ngắt kết nối và kết nối lại
- Kiểm tra server vẫn hoạt động khi một client ngắt kết nối

## Lưu ý bảo mật và giới hạn
- Chỉ chạy local
- Giới hạn kích thước file ảnh
- Kiểm tra tên file tránh path traversal
- Đóng socket/stream đúng cách
- Không để một client lỗi làm ảnh hưởng các client khác
