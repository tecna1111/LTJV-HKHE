# Hướng dẫn phát triển COSRE

Tài liệu này hướng dẫn cài đặt, chạy và kiểm tra source code COSRE trên máy local.

## 1. Yêu cầu môi trường

Cài đặt các công cụ sau:

- Git.
- JDK 17 trở lên.
- Maven 3.9 trở lên, hoặc sử dụng Maven Wrapper có sẵn trong project.
- Node.js 20.19 trở lên.
- npm 10 trở lên.
- MySQL 8 chỉ cần thiết khi chạy profile `mysql`.

Kiểm tra phiên bản:

```bash
java -version
mvn -version
node -v
npm -v
```

## 2. Clone source code

```bash
git clone https://github.com/tecna1111/LTJV-HKHE.git
cd LTJV-HKHE
```

Cấu trúc chính:

```text
LTJV-HKHE/
├── cosre-backend/       Spring Boot REST API
├── cosre-frontend/      React + Vite
├── docs/                Tài liệu dự án
└── CONTRIBUTING.md
```

## 3. Chạy Backend với H2

Profile mặc định là `dev`, sử dụng H2 in-memory nên không cần cài MySQL.

### Windows PowerShell

```powershell
cd cosre-backend
.\mvnw.cmd spring-boot:run
```

Nếu Maven Wrapper không chạy trên máy, sử dụng Maven đã cài:

```powershell
mvn spring-boot:run
```

### macOS/Linux

```bash
cd cosre-backend
chmod +x mvnw
./mvnw spring-boot:run
```

Backend chạy tại:

```text
http://localhost:8080
```

H2 Console:

```text
http://localhost:8080/h2-console
```

Thông tin kết nối H2:

```text
JDBC URL: jdbc:h2:mem:cosre
Username: sa
Password: để trống
```

Tài khoản phát triển mặc định:

```text
Username: admin
Password: Admin@123
```

Tài khoản này chỉ được tự động tạo trong profile `dev` và không được sử dụng trong production.

## 4. Chạy Backend với MySQL

Tạo database trước:

```sql
CREATE DATABASE cosre CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Windows PowerShell

```powershell
cd cosre-backend
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_URL = "jdbc:mysql://localhost:3306/cosre?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
$env:JWT_SECRET = "replace-with-a-secure-secret-key-at-least-32-characters"
$env:CORS_ALLOWED_ORIGIN_PATTERNS = "http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

### macOS/Linux

```bash
cd cosre-backend
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL='jdbc:mysql://localhost:3306/cosre?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USERNAME=root
export DB_PASSWORD=your_password
export JWT_SECRET='replace-with-a-secure-secret-key-at-least-32-characters'
export CORS_ALLOWED_ORIGIN_PATTERNS='http://localhost:5173'
./mvnw spring-boot:run
```

Profile `mysql` không tự tạo tài khoản Admin. Cần seed tài khoản bằng migration, script dữ liệu hoặc bật bootstrap có kiểm soát:

```text
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=change-this-password
```

Tắt bootstrap sau khi tài khoản đầu tiên đã được tạo.

## 5. Chạy Frontend

Mở terminal mới tại thư mục gốc project:

```bash
cd cosre-frontend
npm install
```

Tạo file `.env` từ file mẫu.

### Windows PowerShell

```powershell
Copy-Item .env.example .env
npm run dev
```

### macOS/Linux

```bash
cp .env.example .env
npm run dev
```

Frontend chạy tại:

```text
http://localhost:5173
```

Cấu hình API mặc định trong `.env`:

```dotenv
VITE_API_URL=http://localhost:8080/api/v1
```

Không commit file `.env` hoặc thông tin bí mật lên Git.

## 6. Thứ tự chạy toàn bộ hệ thống

1. Chạy Backend tại port `8080`.
2. Chạy Frontend tại port `5173`.
3. Truy cập `http://localhost:5173`.
4. Đăng nhập bằng tài khoản dev hoặc tài khoản trong MySQL.

## 7. Kiểm tra source trước khi tạo Pull Request

### Backend

```bash
cd cosre-backend
./mvnw test
```

Trên Windows:

```powershell
cd cosre-backend
.\mvnw.cmd test
```

### Frontend

```bash
cd cosre-frontend
npm run lint
npm run build
```

Chỉ tạo Pull Request khi test Backend, lint và build Frontend đều thành công.

## 8. Quy ước tổ chức code

Backend sử dụng cấu trúc module-first:

```text
modules/<module>/
├── controller/          REST endpoints
├── dto/                 Request và response models
├── entity/              JPA entities
├── repository/          Truy cập database
└── service/             Business logic
```

Nguyên tắc Backend:

- Không trả trực tiếp entity chứa dữ liệu nhạy cảm qua API.
- Request/response phải dùng DTO.
- Validate request bằng Jakarta Validation.
- Business logic đặt trong service, không đặt trong controller.
- Repository chỉ chịu trách nhiệm truy cập dữ liệu.
- Exception dùng handler tập trung trong `common/exception`.
- Không hard-code password, JWT secret hoặc thông tin database.

Frontend sử dụng cấu trúc module-first:

```text
src/
├── config/              Cấu hình dùng chung
├── routes/              Router và route guards
├── store/               Global state
└── modules/<module>/
    ├── pages/            Route-level components
    └── *Service.js       API calls của module
```

Nguyên tắc Frontend:

- API call đặt trong service của module.
- URL API đọc từ biến môi trường.
- Page không hard-code token hoặc thông tin đăng nhập.
- Route cần đăng nhập phải đi qua `ProtectedRoute`.
- Chạy ESLint trước khi commit.

## 9. Quy ước Git

Tạo branch riêng cho từng thay đổi:

```bash
git checkout -b feature/ten-tinh-nang
```

Commit theo Conventional Commits:

```text
feat(auth): add refresh token
fix(account): prevent duplicate email
refactor(user): separate response dto
docs: update local setup guide
test(auth): add login service tests
chore: update dependencies
```

Không commit các file sau:

- `.env`.
- Password hoặc API key.
- `node_modules/`.
- `dist/`.
- `target/`.
- IDE configuration cá nhân.

## 10. Lỗi thường gặp

### Port đã được sử dụng

Đổi port Backend:

```powershell
$env:SERVER_PORT = "8081"
```

Sau đó cập nhật `VITE_API_URL` của Frontend tương ứng.

### Frontend không gọi được Backend

Kiểm tra:

- Backend đang chạy.
- `VITE_API_URL` đúng.
- `CORS_ALLOWED_ORIGIN_PATTERNS` chứa URL Frontend.
- Request có header `Authorization: Bearer <token>` với API bảo vệ.

### Không kết nối được MySQL

Kiểm tra database đã tồn tại, MySQL đang chạy và các biến `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` chính xác.

### PowerShell chặn `npm.ps1`

Có thể gọi executable Windows trực tiếp:

```powershell
npm.cmd install
npm.cmd run dev
```
