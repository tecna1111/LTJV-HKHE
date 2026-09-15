# CollabSphere (COSRE)

Nền tảng hỗ trợ học tập theo dự án, xây dựng bằng **Spring Boot và React**. Hệ thống kết nối quản lý học vụ, phê duyệt dự án, làm việc nhóm, cộng tác thời gian thực và đánh giá.

## Chức năng và vai trò

| Vai trò | Chức năng chính |
|---|---|
| `ADMIN` | Quản lý tài khoản: tạo, sửa, khóa/mở khóa, xóa; quản lý báo cáo sự cố |
| `STAFF` | Quản lý môn học, đề cương, lớp và thành viên; import tài khoản/lớp |
| `HEAD_DEPT` | Duyệt hoặc từ chối dự án; phân công dự án cho lớp phù hợp |
| `LECTURER` | Tạo dự án, sử dụng AI gợi ý; quản lý nhóm và trưởng nhóm; câu hỏi cột mốc, tiêu chí, đánh giá; quản lý lịch họp |
| `STUDENT` | Xem lớp/nhóm, nhiệm vụ được giao; Kanban, trả lời câu hỏi cột mốc, đánh giá chéo; cộng tác và tham gia họp |

Quyền thao tác còn phụ thuộc thành viên lớp/nhóm, giảng viên phụ trách và trạng thái đợt đánh giá.

- **Xác thực:** JWT, refresh token, Dashboard theo vai trò.
- **Dự án:** tạo nháp → gửi duyệt → duyệt/từ chối → gán lớp → gán nhóm.
- **Kanban:** task, checklist, sprint, người phụ trách, thời hạn, trọng số, liên kết cột mốc/checkpoint, nhật ký và chỉ số đóng góp.
- **Nhiệm vụ cá nhân:** tổng hợp task được giao, tìm kiếm/lọc, mở đúng task trên Kanban.
- **Cộng tác:** chat, bảng vẽ và tài liệu nhóm có lưu tự động, đồng bộ thời gian thực.
- **Tài nguyên:** upload/download, sửa thông tin, liên kết cột mốc/checkpoint, xóa theo quyền. File mặc định tối đa 25 MB.
- **Đánh giá:** câu hỏi/câu trả lời cột mốc, chấm điểm và phản hồi; tiêu chí, đánh giá chéo, điểm cuối dự án.
- **Meeting:** tạo/sửa/hủy lịch, họp ngay, thông báo/nhắc lịch, phòng Jitsi có JWT.
- **AI:** chat và lịch sử cá nhân/theo ngữ cảnh nhóm; giảng viên xem trước và áp dụng gợi ý dự án/cột mốc.

## Công nghệ và cấu trúc

- Backend: Java 17, Spring Boot 4.1, Spring Security, JPA, Flyway, Maven, MySQL/H2, WebSocket/STOMP.
- Frontend: React 19, Vite 8, React Router, Zustand, Axios, Fabric.js.
- Dịch vụ ngoài: Gemini API và Jitsi có xác thực JWT.

```text
cosre-backend/       REST API, WebSocket, migration và kiểm thử Java
cosre-frontend/      React, giao diện và các module nghiệp vụ
docs/               Hướng dẫn kỹ thuật, MySQL và Jitsi
```

## Chạy đầy đủ trên Windows: MySQL + Jitsi + BE + FE

### 1. Chuẩn bị

- JDK 17 và Maven, hoặc Maven Wrapper có sẵn.
- Node.js đáp ứng Vite: `^20.19.0` hoặc `>=22.12.0`.
- MySQL đang chạy, database `cosre` và tài khoản có quyền truy cập.
- Docker Desktop nếu chạy Jitsi bằng Docker.
- Khóa Gemini có quyền sử dụng model và còn hạn mức để demo AI.

Nếu chưa có database, chạy trong MySQL:

```sql
CREATE DATABASE IF NOT EXISTS cosre CHARACTER SET utf8mb4;
```

Chi tiết cài đặt: [Hướng dẫn MySQL](docs/mysql-setup-guide.md).

### 2. Cấu hình `cosre-backend/.env`

Tạo hoặc cập nhật file này bằng giá trị thật của môi trường. Không ghi đè khóa đã có bằng giá trị mẫu:

```dotenv
SPRING_PROFILES_ACTIVE=mysql
DB_URL=jdbc:mysql://localhost:3306/cosre?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=YOUR_MYSQL_USERNAME
DB_PASSWORD=YOUR_MYSQL_PASSWORD
JWT_SECRET=YOUR_RANDOM_SECRET_AT_LEAST_32_CHARACTERS

GEMINI_API_KEY=YOUR_GEMINI_API_KEY
GEMINI_MODEL=gemini-2.5-flash

MEETING_BASE_URL=https://localhost:8443
MEETING_JITSI_APP_ID=cosre
MEETING_JITSI_SECRET=YOUR_JITSI_JWT_APP_SECRET
MEETING_TOKEN_TTL_SECONDS=7200
```

`GEMINI_MODEL` ở trên là mặc định trong mã nguồn; chọn model thực tế được khóa/project của bạn hỗ trợ.

- Viết tên biến với `_`, không thêm dấu `\` hoặc định dạng liên kết Markdown vào URL.
- Khi đọc `.env` như Java properties bằng lệnh dưới, không bao giá trị trong dấu nháy; ký tự đặc biệt như dấu gạch chéo ngược phải theo cú pháp properties.
- Biến môi trường đã đặt bằng `$env:...` hoặc trong IDE có thể ghi đè file. Mở terminal mới nếu cần bỏ giá trị cũ trong phiên PowerShell.
- `.env` không được Maven/Spring Boot tự đọc nếu không có lệnh import bên dưới.
- Khóa Gemini và secret Jitsi chỉ đặt ở BE, không đặt trong `VITE_*`, không commit hay chia sẻ công khai.

### 3. Bật Jitsi

Cần máy chủ Jitsi đã cấu hình JWT. Trong `.env` của **Jitsi**:

```dotenv
PUBLIC_URL=https://localhost:8443
ENABLE_AUTH=1
ENABLE_GUESTS=0
AUTH_TYPE=jwt
JWT_APP_ID=cosre
JWT_APP_SECRET=YOUR_JITSI_JWT_APP_SECRET
JWT_ACCEPTED_ISSUERS=cosre
JWT_ACCEPTED_AUDIENCES=cosre
JWT_ALLOW_EMPTY=0
```

`JWT_APP_SECRET` phải khớp `MEETING_JITSI_SECRET` ở BE và có ít nhất 32 ký tự. Không dùng nguyên giá trị mẫu.

Sau khi Docker Desktop sẵn sàng, mở terminal tại thư mục Docker Compose của Jitsi:

```powershell
docker compose up -d
docker compose ps
```

Trên máy phát triển đang dùng trong dự án, thư mục đã kiểm tra là:

```powershell
cd D:\jitsi\docker-jitsi-meet-stable-11146-2
docker compose up -d
```

Máy khác thay bằng đường dẫn cài đặt thực tế. Kiểm tra các container `web`, `prosody`, `jicofo`, `jvb` hoạt động và mở `https://localhost:8443` trước khi tham gia qua website. Với chứng chỉ local, trình duyệt cần tin cậy chứng chỉ để phòng nhúng hoạt động.

**Code hiện tại chặn `meet.jit.si` công cộng**, yêu cầu địa chỉ Jitsi riêng dùng HTTPS. Chỉ tạo/sửa lịch không cần phòng Jitsi sẵn sàng; tham gia/họp ngay cần cấu hình JWT hợp lệ. Xem [thiết lập Jitsi đầy đủ](docs/jitsi-secure-setup.md).

### 4. Chạy Backend

Mở PowerShell tại thư mục backend:

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-backend
mvn.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.config.import=file:.env[.properties] --spring.profiles.active=mysql'
```

Nếu không cài Maven, thay `mvn.cmd` bằng `.\mvnw.cmd`.

Kiểm tra log có profile `mysql`, kết nối MySQL thành công và `Started CosreBackendApplication`. Flyway tự áp dụng migration khi khởi động. BE mặc định ở `http://localhost:8080`.

### 5. Chạy Frontend

Mở PowerShell thứ hai:

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-frontend
npm.cmd install
npm.cmd run dev -- --port 5173 --strictPort
```

Mở **http://localhost:5173/login**. `npm.cmd install` cần khi cài lần đầu hoặc cập nhật dependency. FE mặc định gọi `http://localhost:8080/api/v1`; có thể đổi bằng `VITE_API_URL` trước khi chạy Vite.

Profile MySQL mặc định cho phép origin `http://localhost:5173`. Nếu đổi cổng FE, cập nhật `CORS_ALLOWED_ORIGIN_PATTERNS` ở BE tương ứng.

### 6. Tắt dịch vụ

- BE/FE: `Ctrl+C` trong từng terminal.
- Jitsi: `docker compose stop` tại thư mục Compose.
- MySQL giữ dữ liệu qua lần khởi động lại BE. Không xóa database/volume nếu muốn giữ dữ liệu demo.

**Thứ tự chạy:** MySQL → Docker Desktop/Jitsi → BE → FE. AI, chat và cộng tác không cần terminal riêng.

## Chạy nhanh bằng H2 (không cần MySQL)

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-backend
mvn.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

Chạy FE như trên. Profile `dev` dùng database trong bộ nhớ: **dữ liệu tạo thêm mất khi BE dừng**. Profile này tạo sẵn tài khoản, môn `SE101`, đề cương và lớp `SE101-DEMO` có giảng viên cùng hai sinh viên.

AI/Meeting vẫn cần các cấu hình riêng đã nêu; lệnh H2 trên không tự đọc `.env`.

### Tài khoản mẫu

| Vai trò | Username | Password mặc định |
|---|---|---|
| Admin | `admin` | `Admin@123` |
| Trưởng bộ môn | `headdept` | `HeadDept@123` |
| Nhân viên | `staff` | `Staff@123` |
| Giảng viên | `lecturer` | `Lecturer@123` |
| Sinh viên | `student1`, `student2` | `Student@123` |

Chỉ dành cho môi trường phát triển. Có thể ghi đè thông qua biến bootstrap trong `application-dev.properties`.

Trên **MySQL**, mặc định không tạo tài khoản/dữ liệu demo. Dùng tài khoản đã có; nếu cần khởi tạo trên database demo, thêm vào `.env` rồi chạy lại BE:

```dotenv
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_USERS_ENABLED=true
BOOTSTRAP_DEMO_ACADEMIC_ENABLED=true
```

Bootstrap không đặt lại mật khẩu tài khoản đã tồn tại. Có thể tắt các cờ sau khi khởi tạo.

## Luồng demo đề xuất

1. **Admin:** tạo tài khoản phụ, sửa, khóa/mở khóa; giới thiệu báo cáo sự cố.
2. **Staff:** môn học → đề cương → lớp → thêm giảng viên/sinh viên; import bằng file đúng mẫu.
3. **Giảng viên:** tạo dự án, dùng AI gợi ý, xem trước/áp dụng, gửi duyệt.
4. **Trưởng bộ môn:** duyệt dự án. Gán dự án đã duyệt cho lớp cùng môn học.
5. **Giảng viên:** tạo nhóm, thêm thành viên/trưởng nhóm, chọn đề tài đã gán cho lớp.
6. **Nhóm:** mở Workspace, tạo task, giao người thực hiện, thêm checklist; đổi To-do → In Progress → Done. Xem nhiệm vụ cá nhân và chỉ số.
7. **Hai phiên trình duyệt:** chat, cùng sửa tài liệu, cùng vẽ bảng; kiểm tra lưu và đồng bộ.
8. **Giảng viên/trưởng nhóm:** tạo lịch hoặc họp ngay; thành viên tham gia, thử microphone/camera/chia sẻ màn hình.
9. **Tài nguyên/cột mốc:** upload báo cáo; giảng viên tạo câu hỏi, sinh viên trả lời, giảng viên chấm và phản hồi.
10. **Đánh giá:** tạo tiêu chí → mở đợt → sinh viên đánh giá chéo → giảng viên xem tổng hợp, nhập điểm cuối và khóa đợt.

Dùng các profile trình duyệt riêng cho mỗi tài khoản. Trên cùng máy, tắt microphone/loa ở một phiên họp để tránh tiếng vọng.

### Phân biệt các loại điểm và tiến độ

- Tick checklist không tự chuyển task sang Done. Còn checklist chưa hoàn thành thì không được chuyển Done.
- Tiến độ Kanban và tiến độ milestone là hai chỉ số khác nhau.
- Chỉ số đóng góp Kanban hiện là **bản xem trước**, không dùng làm điểm chính thức.
- Điểm câu trả lời cột mốc được lưu riêng trong `milestone_answers.score`; chưa tự cộng vào điểm cuối dự án.
- Điểm cuối dự án do giảng viên nhập riêng.
- Trang `/evaluations/feedback` hiện cần mã câu trả lời; luồng hỏi/trả lời trực tiếp có tại Workspace → Câu hỏi cột mốc.

## Đường dẫn chính

| Chức năng | Đường dẫn FE |
|---|---|
| Dashboard | `/dashboard` |
| Quản lý tài khoản | `/admin/users` |
| Báo cáo sự cố | `/admin/reports` |
| Môn học / lớp | `/staff/subjects`, `/staff/classrooms` |
| Import | `/staff/accounts/import`, `/staff/classrooms/import` |
| Luồng dự án / tạo dự án | `/workflow`, `/lecturer/projects/new` |
| Nhóm / nhóm sinh viên | `/teams`, `/student/teams` |
| Nhiệm vụ cá nhân | `/student/tasks` |
| Workspace | `/teams/:id/workspace` |
| Kanban / bảng vẽ / tài liệu | `/teams/:id/kanban`, `/teams/:id/whiteboard`, `/teams/:id/document` |
| Câu hỏi cột mốc | `/teams/:id/milestone-questions` |
| Chat / Meeting / tài nguyên | `/messages`, `/meetings`, `/resources` |
| Tiêu chí / đánh giá chéo | `/evaluations/criteria`, `/peer-evaluations` |
| Tổng hợp / điểm cuối / phản hồi | `/evaluations/summary`, `/evaluations/final`, `/evaluations/feedback` |

`:id` là ID nhóm thật. Các trang và API kiểm tra quyền người dùng.

## Lỗi thường gặp

| Hiện tượng | Kiểm tra |
|---|---|
| Không có dữ liệu MySQL cũ | Log có profile `mysql` không? Có dùng đúng database/cổng không? |
| MySQL `Access denied` | Username/password database, biến môi trường ghi đè `.env` |
| Website `Access Denied` | Quyền tài khoản, lớp/nhóm/dự án; kiểm tra API lỗi trong Network, không nhầm với mật khẩu MySQL |
| AI chưa cấu hình API key | Đã import `.env` và khởi động lại BE chưa? |
| AI vượt giới hạn yêu cầu | Kiểm tra hạn mức project/model; restart BE không làm mới hạn mức |
| Meeting chưa cấu hình JWT an toàn | App ID phải có giá trị; secret ít nhất 32 ký tự, khớp Jitsi |
| Có JWT nhưng không vào phòng | HTTPS/chứng chỉ, Jitsi đang chạy, App ID/secret và domain phải khớp |
| Không thấy đề tài để gán nhóm | Dự án đã duyệt và đã gán cho đúng lớp chưa? |
| Kanban trống | Kiểm tra bộ lọc người phụ trách/sprint; tạo task và phân công thành viên |
| Chờ mở đợt đánh giá | Giảng viên cần mở đợt cho đúng nhóm/dự án và chuẩn bị tiêu chí |
| Không có email | Email mặc định tắt; cần cấu hình mail riêng. Thông báo trong ứng dụng không yêu cầu email thật |
| Cổng FE bận | Dừng tiến trình FE cũ hoặc đổi cổng và cập nhật CORS |

## Kiểm tra mã nguồn

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-backend
mvn.cmd test
```

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-frontend
npm.cmd run lint
npm.cmd run build
```

Build/lint không thay thế kiểm thử trình duyệt, MySQL, AI và Jitsi thật. Sau cập nhật, kiểm tra lại luồng demo trên môi trường của bạn.

