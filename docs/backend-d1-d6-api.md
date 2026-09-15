# Backend D1–D6: API và quy tắc tích hợp

Base URL: `/api/v1`. Các API yêu cầu JWT. Danh tính người thao tác lấy từ phiên đăng nhập.

## D1 — Tiêu chí

Giữ các API `/evaluations/criteria` hiện có. Chỉ giảng viên tạo dự án được tạo/sửa/xóa tiêu chí của dự án. Quyền đọc dành cho chủ dự án và giảng viên/thành viên nhóm được giao dự án.

- `maxScore`: lớn hơn 0, không quá 100; `weight`: lớn hơn 0, không quá 1; tối đa 2 chữ số thập phân, phù hợp schema hiện tại.
- Khi soạn bộ tiêu chí, tổng trọng số không vượt 1. Khi nộp đánh giá, tổng phải đúng 1 và phải gửi đủ mỗi tiêu chí đúng một lần.
- Khi đã có bài đánh giá, không được thêm tiêu chí. Tiêu chí đã được dùng không được đổi trọng số/thang điểm hoặc xóa; có thể sửa tiêu đề/mô tả.
- Response `locked` cho biết tiêu chí đã được sử dụng.
- Các thao tác chấm/đổi rubric/khóa đợt dùng khóa giao dịch trên bản ghi dự án để tuần tự hóa cập nhật.

## D2 — Peer review và đợt cuối dự án

Sinh viên chỉ đánh giá sinh viên khác trong cùng nhóm; nhóm phải được giao đúng `projectId`. `milestoneId`, nếu có, phải thuộc dự án. Điểm từng tiêu chí phải trong khoảng 0 đến `maxScore`.

- `POST /evaluations/peer/open-final?teamId=...&projectId=...`: giảng viên quản lý nhóm mở đợt cuối dự án.
- `POST /evaluations/peer`: giữ request cũ. `milestoneId: null` là đánh giá cuối dự án, chỉ nộp được sau khi mở đợt.
- Nộp lại cùng người chấm/người được chấm/nhóm/dự án/milestone cập nhật bài cũ.
- `POST /evaluations/peer/lock?teamId=...&projectId=...`: khóa đợt, kể cả nhóm chưa có bài chấm. Chặn cả sửa và nộp mới; không có API mở khóa.
- `/given`, `/received`: dữ liệu của chính người đăng nhập, trong dự án có quyền truy cập.
- `/summary/student/{studentId}` và `/summary/team/{teamId}`: chỉ giảng viên quản lý nhóm; sinh viên phải thuộc nhóm. Danh sách thành viên tổng hợp lấy từ database; tham số `memberIds` cũ được chấp nhận nhưng không dùng để quyết định phạm vi.
- Tổng kết chỉ tính bài cuối dự án `SUBMITTED`/`LOCKED`; bài milestone không bị trộn vào tổng kết. Điểm: `sum(score / maxScore * weight * 10)`, trung bình trên các bài được nhận; không có bài trả 0 và số bài bằng 0.

Do dự án hiện có trạng thái duyệt (`DRAFT/PENDING/APPROVED/DENIED`), chưa có trạng thái kết thúc, thao tác mở đợt của giảng viên là mốc cho phép đánh giá cuối dự án.

## D3 — Giảng viên chấm cuối dự án; feedback theo answerId

`PUT /evaluations/final`, chỉ giảng viên quản lý nhóm, trong đợt cuối dự án đã mở và chưa khóa:

```json
{"teamId": 12, "projectId": 5, "studentId": null, "score": 8.5, "feedback": "Nhận xét nhóm"}
```

`studentId: null` chấm nhóm; truyền ID sinh viên của nhóm để chấm cá nhân. Thang điểm 0–10. Gửi lại cập nhật bản ghi tương ứng. Điểm nhóm và cá nhân được lưu riêng, không tự áp dụng một công thức cộng điểm chưa được thống nhất.

`GET /evaluations/final?teamId=12&projectId=5`: giảng viên xem cả nhóm; sinh viên xem điểm nhóm và điểm cá nhân của chính mình.

Quy cung cấp `answerId` của bản ghi trong `milestone_answers`:

- `PUT /evaluations/answers/{answerId}/peer-feedback` với `{"feedback":"Nhận xét"}`.
- `GET /evaluations/answers/{answerId}/peer-feedback`.

Backend truy ngược answer → question → milestone → project và nhóm; không tin project/team do client gửi. Chỉ thành viên cùng nhóm được phản hồi câu trả lời của người khác; không phản hồi câu trả lời của chính mình. Mỗi người có một phản hồi trên mỗi answer, được cập nhật trước khi khóa đợt. Giảng viên quản lý nhóm và các thành viên được đọc phản hồi.

## D4 — Tài nguyên

Giữ upload multipart `/resources/classroom/{classroomId}` và `/resources/team/{teamId}`. Quyền được kiểm tra trong service trước khi ghi file.

- Tài liệu lớp: Admin/Staff hoặc giảng viên được phân công lớp được upload/quản lý; sinh viên trong lớp được đọc.
- File nhóm: thành viên và giảng viên quản lý nhóm được upload/đọc. Admin/Staff có quyền quản trị tài nguyên qua service; endpoint upload nhóm vẫn chỉ dành cho Student/Lecturer.
- Xóa/sửa metadata: người upload còn quyền truy cập, giảng viên quản lý phạm vi đó, hoặc Admin/Staff.
- Danh sách, metadata và download đều kiểm tra phạm vi; không chỉ kiểm tra đăng nhập.
- `GET /resources/{id}` lấy metadata, không trả đường dẫn lưu nội bộ.
- `PUT /resources/{id}/metadata` thay metadata và liên kết:

```json
{"title":"Bài nộp","description":"Tài liệu nhóm","milestoneId":7,"checkpointId":15}
```

Liên kết milestone/checkpoint chỉ áp dụng cho file nhóm. Checkpoint phải thuộc nhóm; milestone phải thuộc dự án của nhóm. Khi có checkpoint, milestone được suy ra từ checkpoint; nếu đồng thời truyền milestone không khớp thì từ chối. Hai ID null gỡ liên kết. Có thể upload trước, sau đó gọi API metadata để gắn liên kết.

File được lưu thống nhất qua `ResourceService`, tại `app.upload.dir` (mặc định `uploads`), dưới tên UUID. Giới hạn dung lượng kiểm tra cả metadata và luồng dữ liệu. Chặn đường dẫn thoát thư mục và symlink của file. Upload thất bại/transaction rollback dọn file vừa tạo; xóa file vật lý chỉ thực hiện sau commit xóa metadata. Nếu xóa vật lý lỗi, ghi log để vận hành dọn lại; chưa có hàng đợi retry bền vững.

## D5 — Báo cáo sự cố và sự kiện email

- `POST /incidents` với `{"title":"Lỗi upload","description":"Các bước tái hiện..."}`: lưu trạng thái `OPEN`, trả 201.
- `GET /incidents`: người thường xem báo cáo của mình; Admin xem tất cả.
- `GET /incidents/{id}`: người gửi hoặc Admin.
- `PATCH /incidents/{id}/status`: chỉ Admin, ví dụ `{"status":"IN_PROGRESS","resolution":null}`.

Luồng trạng thái: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`. Cho phép `RESOLVED → IN_PROGRESS` khi cần xử lý lại. `RESOLVED/CLOSED` yêu cầu nội dung resolution; có optimistic locking chống ghi đè đồng thời.

Khi tạo báo cáo, backend phát `IncidentReportedEvent(reportId, reporterId, title, createdAt)`. Công Duy tích hợp listener:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onIncidentReported(IncidentReportedEvent event) {
    // Dùng dịch vụ email của module thông báo để gửi tới các Admin đang hoạt động.
}
```

Sự kiện chỉ là điểm tích hợp trong tiến trình, chưa có email listener/SMTP hay outbox/retry bền vững trong phần này. Chưa xác nhận email đã được gửi. Đây là API báo cáo sự cố; không liên quan xuất điểm Excel/PDF.

## D6 — QA và trách nhiệm module

Xem `backend-d1-d6-qa.md` cho kết quả kiểm tra. Mỗi chủ module vẫn sửa lỗi và bổ sung test của module mình; tài liệu này tổng hợp các phần đã kiểm tra và các điểm cần kiểm tra tích hợp.

Migration mới: `V11__evaluation_resources_incidents.sql`. Giữ nguyên V1–V10. V11 bổ sung schema, phạm vi unique peer review theo nhóm và chuyển trạng thái khóa các bài cũ sang khóa đợt.
