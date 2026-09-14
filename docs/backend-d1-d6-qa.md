# QA backend D1–D6

Ngày kiểm tra: 2026-09-14. Môi trường: Windows, Java 21 (biên dịch target Java 17), Maven Wrapper, Spring Boot 4.1, H2 chế độ MySQL.

## Kết quả tự động

Lệnh chạy tại `cosre-backend`: `./mvnw.cmd test -q`.

Kết quả: **72 tests, 0 failures, 0 errors, 0 skipped**.

Các nhóm test mới/bổ sung:

- D1: chặn thêm tiêu chí sau khi đã có bài; chặn đổi thang/xóa tiêu chí đã dùng; cho phép sửa mô tả; chặn giảng viên không sở hữu dự án.
- D2: không tự chấm, từ chối người ngoài nhóm và sai dự án, chặn giảng viên không quản lý nhóm; yêu cầu mở đợt cuối dự án; chặn nộp mới sau khóa dù chưa có bài trước đó; từ chối tiêu chí trùng/khác dự án và tổng trọng số thiếu; kiểm tra điểm vượt thang; kiểm tra điểm tổng hợp không gồm milestone.
- D3: chấm cuối dự án yêu cầu đợt mở/chưa khóa; chấm cá nhân kiểm tra thành viên; feedback tra answerId và không cho tự phản hồi.
- D4: giới hạn file, phạm vi lớp và download nhóm, đường dẫn thoát thư mục, dọn file khi lưu metadata thất bại, checkpoint sai nhóm và upload của giảng viên ngoài lớp.
- D5: tạo báo cáo và phát sự kiện; người thường không xử lý sự cố, không đọc báo cáo người khác; kiểm tra chuyển trạng thái và bắt buộc resolution.

Test tích hợp `EvaluationWorkflowTests` dùng Spring, JPA và database thật H2, có flush/clear để đọc lại dữ liệu:

- Nộp rồi sửa peer review: cùng ID, chỉ một bài; điểm tổng hợp đọc lại đúng.
- Điểm nhóm và điểm cá nhân lưu riêng; sinh viên không đọc điểm cá nhân người khác.
- Peer feedback trên answer đã lưu, cập nhật cùng bản ghi.
- Khóa nhóm chưa có bài vẫn từ chối bài nộp đầu tiên.

Ứng dụng khởi động thành công, Flyway áp dụng V1–V11 và Hibernate validate schema thành công. Các test sẵn có của account, authentication, classroom, project, subject, chat, collaboration và notification cũng đạt trong cùng lần chạy.

## Phạm vi chưa xác nhận

- Chưa chạy trên MySQL server thực tế, chưa thử tải đồng thời trên MySQL hoặc nâng cấp từ dữ liệu production. Cần chủ module/database kiểm tra thêm trước triển khai.
- Chưa kiểm thử end-to-end frontend qua HTTP/JWT; test mới tập trung service, persistence và nghiệp vụ.
- Chưa có listener email/SMTP của Công Duy trong repo; đã có sự kiện `IncidentReportedEvent` và hướng dẫn `AFTER_COMMIT`. Chưa xác nhận email đến Admin. Sự kiện chưa có outbox/retry bền vững.
- Chưa xác nhận API trả answerId phía Quy; test dùng bản ghi answer theo schema hiện có.
- Khi xóa vật lý sau commit thất bại, có log để dọn lại; chưa có retry tự động.

## Bàn giao và trách nhiệm

- Chủ module D1–D3 duy trì test đánh giá/feedback và các chính sách mở/khóa; thống nhất chính sách mở đợt với giảng viên nghiệp vụ.
- Chủ module D4 duy trì test quyền và lưu file; xác nhận thư mục upload bền vững ở môi trường triển khai.
- Quy xác nhận answerId trả cho frontend tương ứng `milestone_answers.id`.
- Công Duy tích hợp listener gửi email tới Admin; tự bổ sung test email thành công/thất bại và không gửi khi rollback.
- Mỗi chủ module tiếp tục tự sửa lỗi và bổ sung test của mình. Báo cáo QA này không thay thế kiểm thử các luồng chưa được chạy.

Hợp đồng API và ví dụ request: [backend-d1-d6-api.md](backend-d1-d6-api.md).
