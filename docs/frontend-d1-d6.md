# Frontend đánh giá, tài nguyên và sự cố

Các màn hình đã nối API:

- `/evaluations/criteria`: giảng viên chọn dự án của mình; tạo/sửa/xóa tiêu chí, kiểm tra tổng trọng số và khóa thang điểm/trọng số đã dùng.
- `/evaluations/final`: chọn nhóm; giảng viên mở/khóa đợt, chấm nhóm hoặc từng thành viên. Sinh viên xem kết quả được cấp quyền.
- `/evaluations/feedback?answerId=123`: đọc/gửi lại phản hồi theo mã câu trả lời. Có thể nhập mã khi không mở từ liên kết milestone. Module của Quy cần truyền answerId vào liên kết này.
- `/resources`: chọn lớp/nhóm; upload, download, xóa và sửa thông tin file. Chọn milestone/checkpoint trong biểu mẫu sửa. Chặn nút quản lý khi không có quyền tương ứng.
- `/incidents`: gửi và xem báo cáo của mình. `/admin/reports`: Admin xem danh sách, lọc và cập nhật trạng thái/kết quả xử lý.

Menu Dashboard có lối vào theo vai trò. Trang đánh giá chéo đã thêm chọn nhóm, đọc trạng thái đợt và lọc dữ liệu cuối dự án đúng nhóm. Tổng hợp không suy đoán trạng thái khóa từ số bài chấm.

API bổ sung: `GET /api/v1/evaluations/peer/round?teamId=...&projectId=...`, chỉ thành viên/giảng viên quản lý nhóm được đọc. Checkpoint hiện dùng `/api/checkpoints`; frontend giữ đúng đường dẫn backend hiện có.

Kiểm tra: `npm run lint`, `npm run build`, Maven backend test. Build có cảnh báo bundle lớn hơn 500 KB; không chặn build. Chưa chạy kiểm thử tương tác trình duyệt hoặc email thật. Việc tiếp nhận báo cáo không phụ thuộc cấu hình SMTP.
