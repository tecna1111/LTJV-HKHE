# Hướng dẫn chạy và đóng góp COSRE

Tài liệu này hướng dẫn thiết lập môi trường, chạy toàn bộ hệ thống và kiểm tra source trước khi đóng góp code.

## Yêu cầu môi trường

- Git
- JDK 17 trở lên
- Maven 3.9 trở lên hoặc Maven Wrapper trong project
- Node.js 20.19 trở lên
- npm 10 trở lên
- MySQL 8 (chỉ cần khi chạy profile `mysql`)

Kiểm tra phiên bản:

```powershell
git --version
java -version
mvn -version
node --version
npm.cmd --version
```

## Lấy source code

```powershell
git clone https://github.com/tecna1111/LTJV-HKHE.git
cd LTJV-HKHE
```

## Chạy web nhanh với H2

Đây là cách được khuyến nghị cho môi trường development. H2 chạy trong bộ nhớ nên không cần cài đặt database riêng.

### Bước 1: chạy Backend

Mở PowerShell thứ nhất:

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-backend
.\mvnw.cmd spring-boot:run
```

Nếu Maven Wrapper gặp lỗi:

```powershell
mvn spring-boot:run
```

Chờ đến khi terminal hiển thị ứng dụng đã `Started`. Backend chạy tại:

```text
http://localhost:8080
```

### Bước 2: chạy Frontend

Mở PowerShell thứ hai:

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-frontend
npm.cmd install
Copy-Item .env.example .env -ErrorAction SilentlyContinue
npm.cmd run dev
```

Vite sẽ hiển thị URL thực tế, ví dụ:

```text
http://localhost:5173
```

Nếu port này bận, Vite tự chọn port tiếp theo như `5174` hoặc `5175`. Hãy truy cập đúng URL được in trong terminal.

### Bước 3: đăng nhập

Mở `/login`, chọn vai trò Admin và nhập:

```text
Username: admin
Password: Admin@123
```

Đăng nhập thành công sẽ chuyển đến `/dashboard`.

## Cấu hình H2 Console

Truy cập:

```text
http://localhost:8080/h2-console
```

Thông tin kết nối:

```text
Driver Class: org.h2.Driver
JDBC URL:     jdbc:h2:mem:cosre
Username:     sa
Password:     để trống
```

Nhấn `Connect` để xem bảng `USERS`. Dữ liệu H2 sẽ mất khi Backend dừng.

## Chạy Backend với MySQL

### Bước 1: tạo database

```sql
CREATE DATABASE cosre
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

### Bước 2: khai báo biến môi trường

Windows PowerShell:

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-backend

$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_URL = "jdbc:mysql://localhost:3306/cosre?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
$env:JWT_SECRET = "replace-with-a-secure-secret-key-at-least-32-characters"
$env:CORS_ALLOWED_ORIGIN_PATTERNS = "http://localhost:5173"

.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
cd cosre-backend
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL='jdbc:mysql://localhost:3306/cosre?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USERNAME=root
export DB_PASSWORD='your_password'
export JWT_SECRET='replace-with-a-secure-secret-key-at-least-32-characters'
export CORS_ALLOWED_ORIGIN_PATTERNS='http://localhost:5173'
./mvnw spring-boot:run
```

### Tạo Admin đầu tiên trong MySQL

Profile `mysql` mặc định không tự tạo Admin. Có thể bật bootstrap tạm thời:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED = "true"
$env:BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
$env:BOOTSTRAP_ADMIN_PASSWORD = "change-this-password"
```

Sau khi Admin được tạo thành công, dừng Backend và tắt bootstrap:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED = "false"
```

Không sử dụng password mặc định trong môi trường thật.

## Biến môi trường

### Backend

| Biến | Mặc định development | Ý nghĩa |
|---|---|---|
| `SERVER_PORT` | `8080` | Port Backend |
| `SPRING_PROFILES_ACTIVE` | `dev` | Profile `dev` hoặc `mysql` |
| `JWT_SECRET` | Development secret | Khóa ký JWT, production phải thay đổi |
| `JWT_EXPIRATION_MS` | `86400000` | Thời hạn JWT, mặc định 24 giờ |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | Localhost mọi port | Origin được phép gọi API |
| `DB_URL` | MySQL local | JDBC URL cho profile MySQL |
| `DB_USERNAME` | `root` | Username MySQL |
| `DB_PASSWORD` | Trống | Password MySQL |

### Frontend

File `.env`:

```dotenv
VITE_API_URL=http://localhost:8080/api/v1
```

Khi thay đổi `.env`, cần restart Vite development server.

## API đăng nhập

### Đăng nhập

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@123"
}
```

Đăng nhập thành công trả HTTP `200`. Sai thông tin đăng nhập trả HTTP `401`.

### Lấy người dùng hiện tại

```http
GET /api/v1/auth/me
Authorization: Bearer <token>
```

## Kiểm tra trước khi commit

Backend:

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-frontend
npm.cmd run lint
npm.cmd run build
```

Các kiểm tra trên phải thành công trước khi tạo Pull Request.

## Quy ước tổ chức code

Backend:

```text
modules/<module>/
|-- controller/     REST API và HTTP mapping
|-- dto/            Request/response models
|-- entity/         JPA entities
|-- repository/     Truy cập database
`-- service/        Business logic
```

- Controller không chứa business logic.
- API sử dụng DTO, không trả entity có dữ liệu nhạy cảm.
- Request phải được validate bằng Jakarta Validation.
- Exception được xử lý tập trung trong `common/exception`.
- Password phải được mã hóa; không lưu plain text.

Frontend:

```text
src/
|-- config/         Cấu hình Axios
|-- routes/         Router và ProtectedRoute
|-- store/          Global state
`-- modules/
    `-- <module>/   Page, component và service theo nghiệp vụ
```

- API call đặt trong service của module.
- Route riêng tư phải đi qua `ProtectedRoute`.
- URL Backend phải đọc từ biến môi trường.
- Không hard-code JWT hoặc thông tin nhạy cảm trong component.

## Quy trình Git

Tạo nhánh từ `main`:

```powershell
git switch main
git pull origin main
git switch -c feature/ten-tinh-nang
```

Kiểm tra và commit:

```powershell
git status
git add .
git commit -m "feat(auth): hoàn thiện luồng đăng nhập"
git push -u origin feature/ten-tinh-nang
```

Các loại commit thường dùng:

- `feat`: thêm chức năng.
- `fix`: sửa lỗi.
- `refactor`: tái cấu trúc code.
- `docs`: cập nhật tài liệu.
- `test`: thêm hoặc sửa test.
- `chore`: cấu hình và dependency.

Không commit `.env`, password, API key, JWT secret, `node_modules`, `dist` hoặc `target`.

## Lỗi thường gặp

### `OPTIONS /login` trả 403

Nguyên nhân thường là Frontend đang chạy ở port khác origin được Backend cho phép. Trong profile `dev`, Backend mặc định cho phép `localhost` và `127.0.0.1` ở mọi port.

Sau khi sửa cấu hình CORS, phải restart Backend.

### Frontend không gọi được Backend

Kiểm tra:

1. Backend đã chạy tại port `8080`.
2. `VITE_API_URL` trỏ đến `http://localhost:8080/api/v1`.
3. Frontend origin nằm trong `CORS_ALLOWED_ORIGIN_PATTERNS`.
4. Reload trình duyệt bằng `Ctrl + Shift + R`.

### PowerShell chặn `npm.ps1`

Sử dụng executable Windows:

```powershell
npm.cmd install
npm.cmd run dev
```

### Port đang được sử dụng

Đổi port Backend:

```powershell
$env:SERVER_PORT = "8081"
.\mvnw.cmd spring-boot:run
```

Sau đó cập nhật `VITE_API_URL` tương ứng và restart Frontend.

## Liên hệ và Pull Request

Pull Request cần mô tả rõ:

- Vấn đề hoặc chức năng được xử lý.
- Thay đổi chính ở Backend/Frontend.
- Cách kiểm thử.
- Ảnh giao diện nếu có thay đổi UI.
