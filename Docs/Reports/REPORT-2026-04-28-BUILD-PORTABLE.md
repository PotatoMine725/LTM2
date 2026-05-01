# Báo cáo build và đóng gói portable ngày 2026-04-28

## Kết quả kiểm tra build
- Đã xác nhận Maven hoạt động khi gọi trực tiếp từ `D:\Tools\apache-maven-3.9.9\bin\mvn.cmd`
- Build project thành công bằng lệnh `clean package`
- File jar đã được tạo trong `target`

## Kết quả đóng gói portable
- Đã tạo bản portable bằng `jpackage`
- Kết quả nằm trong thư mục `dist`
- Có hai bản riêng:
  - `dist\LTM2-Client`
  - `dist\LTM2-Server`
- Mỗi bản có runtime Java đi kèm, có thể chạy trên máy không cài Java hoặc Maven

## Ghi chú
- File portable hiện là app-image dạng thư mục, không phải file nén đơn lẻ
- Nếu cần, có thể đóng gói tiếp thành `.zip` hoặc `.msi`
