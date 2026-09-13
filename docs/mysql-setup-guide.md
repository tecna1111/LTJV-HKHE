# Hướng dẫn cấu hình MySQL cho CollabSphere (COSRE) — T1 (Dev 2 - Toán)

Tài liệu này giúp bất kỳ ai trong nhóm dựng được backend chạy trên **MySQL thật**
(không phải H2 in-memory) trong vài phút, và biết cách tạo tài khoản demo đủ 5 vai trò.

## 1. Vì sao cần việc này

Mặc định `mvn spring-boot:run` chạy với profile `dev` → dùng H2 in-memory (mất dữ liệu
mỗi lần tắt app). H2 chạy ở `MODE=MySQL` chỉ giả lập cú pháp gần giống MySQL, **không
đảm bảo 100% giống MySQL thật** (charset, collation, một số kiểu dữ liệu, giới hạn
độ dài, hành vi transaction...). Trước khi release / demo chung, bắt buộc phải chạy
thử toàn bộ migration (`V1` → `V10` và các bản sau này) trên MySQL thật.

**Đã xác minh (10/09/2026):** toàn bộ 10 migration hiện có (`V1__users_baseline.sql`
đến `V10__checkpoint_and_milestone_questions.sql`) áp dụng thành công, không lỗi cú
pháp, trên MySQL 8 / MariaDB 10.11 — tạo đủ 25 bảng, tất cả foreign key resolve đúng.

## 2. Cách 1 (khuyên dùng): Docker Compose — 1 lệnh có ngay MySQL

Yêu cầu: đã cài Docker Desktop (hoặc Docker Engine + Compose plugin).

```bash
cd cosre-backend
cp .env.example .env          # chỉnh lại mật khẩu nếu muốn
docker compose -f docker-compose.mysql.yml up -d
```

Kiểm tra MySQL đã sẵn sàng:
```bash
docker compose -f docker-compose.mysql.yml ps
# STATUS phải là "healthy"
```

Để dừng (dữ liệu vẫn được giữ trong volume `cosre_mysql_data`):
```bash
docker compose -f docker-compose.mysql.yml down
```

Để xoá sạch làm lại từ đầu (mất toàn bộ dữ liệu MySQL local):
```bash
docker compose -f docker-compose.mysql.yml down -v
```

## 3. Cách 2: Cài MySQL/MariaDB trực tiếp trên máy

```bash
# Ubuntu/Debian/WSL
sudo apt-get update
sudo apt-get install -y mysql-server   # hoặc: mariadb-server

sudo service mysql start               # hoặc: sudo service mariadb start

sudo mysql -uroot <<'SQL'
CREATE DATABASE IF NOT EXISTS cosre CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'cosre_app'@'localhost' IDENTIFIED BY 'CosreApp@123';
GRANT ALL PRIVILEGES ON cosre.* TO 'cosre_app'@'localhost';
FLUSH PRIVILEGES;
SQL
```

Windows/macOS: cài MySQL Community Server bản cài đặt sẵn (installer/Homebrew), sau
đó chạy đoạn SQL ở trên qua MySQL Workbench hoặc `mysql` CLI.

## 4. Chạy backend với profile `mysql`

Không cần chạy tay từng migration — **Flyway tự động áp dụng khi Spring Boot khởi
động** (`spring.flyway.enabled=true` đã có sẵn trong `application.properties`).

```bash
cd cosre-backend
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL="jdbc:mysql://localhost:3306/cosre?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME=cosre_app
export DB_PASSWORD=CosreApp@123
export JWT_SECRET=change-this-to-a-random-32-plus-character-secret
export BOOTSTRAP_ADMIN_ENABLED=true
export BOOTSTRAP_USERS_ENABLED=true

./mvnw spring-boot:run
```

> Lưu ý: file `.env.example`/`.env` **chỉ được `docker compose` tự đọc**. Spring Boot
> (Java) không tự đọc file `.env` — nếu không dùng IDE, phải `export` từng biến như
> trên (hoặc `export $(grep -v '^#' .env | xargs)` để export nhanh cả file), hoặc set
> Environment Variables trong Run Configuration của IntelliJ/VS Code.

Nếu chạy đúng, log sẽ có dòng kiểu:
```
Flyway ... Successfully applied 10 migrations to schema `cosre`
```

## 5. Tài khoản demo có sẵn (đủ 5 vai trò)

Khi `BOOTSTRAP_ADMIN_ENABLED=true` và `BOOTSTRAP_USERS_ENABLED=true`, app tự tạo (nếu
chưa tồn tại) các tài khoản sau — dùng ngay để test mọi luồng theo role mà không cần
tạo tay qua UI:

| Vai trò | Username | Mật khẩu mặc định |
|---|---|---|
| ADMIN | `admin` | `Admin@123` |
| HEAD_DEPT | `headdept` | `HeadDept@123` |
| STAFF | `staff` | `Staff@123` |
| LECTURER | `lecturer` | `Lecturer@123` |
| STUDENT | `student1` | `Student@123` |
| STUDENT | `student2` | `Student@123` |

Đổi username/mật khẩu qua biến môi trường tương ứng (`BOOTSTRAP_LECTURER_USERNAME`,
`BOOTSTRAP_STUDENT1_PASSWORD`, ...) nếu cần — xem đầy đủ trong `DataInitializer.java`.

## 6. Dữ liệu học vụ demo (tuỳ chọn)

Set thêm `BOOTSTRAP_DEMO_ACADEMIC_ENABLED=true` (mặc định `true` sẵn trên `dev`
(H2), mặc định `false` trên `mysql` để không tự tạo rác trên DB dùng chung) để có
luôn:

- Subject `SE101` — "Nhập môn Công nghệ phần mềm"
- Syllabus 1.0 gắn với môn trên
- Classroom `SE101-DEMO`, học kỳ HK1 2025-2026, đã gán sẵn `lecturer` làm giảng viên
  và `student1`, `student2` làm sinh viên

→ Các module khác (Team, Project, Checkpoint...) có ngay dữ liệu nền để test tích hợp
mà không phải tự tạo lại từ đầu qua UI mỗi lần dựng môi trường mới.

**Chỉ bật cờ này một lần khi seed cho môi trường demo/staging dùng chung**, rồi tắt
lại để tránh vô tình seed lặp mỗi lần ai đó khởi động lại app trên cùng một DB.

## 7. Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách sửa |
|---|---|---|
| `Public Key Retrieval is not allowed` | MySQL 8 dùng `caching_sha2_password` | Thêm `&allowPublicKeyRetrieval=true` vào `DB_URL` (đã có sẵn trong ví dụ trên) |
| `Unknown database 'cosre'` | Chưa tạo database | Chạy lại bước tạo DB ở mục 2/3 |
| `Access denied for user` | Sai user/password hoặc user chưa có quyền | Kiểm tra lại `GRANT ALL PRIVILEGES` đã chạy đúng chưa |
| Flyway báo `Validate failed: Migration checksum mismatch` | Có ai đó **sửa lại nội dung** 1 file migration đã từng chạy | Không được sửa migration cũ — luôn tạo file `V<n+1>__...sql` mới (xem mục 8) |
| App start OK nhưng thiếu bảng | Quên `spring.flyway.enabled=true` hoặc dùng nhầm `ddl-auto=update` | Dự án dùng `ddl-auto=validate` — schema luôn phải đến từ Flyway, không phải Hibernate tự sinh |

## 8. Quy ước số phiên bản migration (Toán điều phối)

Theo bảng phân công, **Toán (Dev 2) là người điều phối số version migration** để
tránh trùng (đã từng xảy ra: `V9` vừa được dùng cho Resource vừa định dùng cho
Checkpoint, phải đổi thành `V10`). Quy trình đề xuất:

1. Trước khi tạo migration mới, nhắn vào kênh chung nhóm: *"Mình xin V11 cho module
   X"*.
2. Toán xác nhận số hiện tại lớn nhất đang có trên `develop`/`main`, cấp số tiếp theo.
3. Đặt tên file đúng format `V<n>__<mo_ta_ngan>.sql`, viết bằng snake_case, tiếng
   Anh không dấu.
4. **Tuyệt đối không sửa nội dung file migration đã merge vào `develop`/`main`** — dù
   chỉ đổi 1 ký tự, Flyway sẽ báo lỗi checksum mismatch cho bất kỳ ai đã chạy bản cũ.
   Cần sửa gì thì tạo migration mới để `ALTER`/`UPDATE` tiếp.
