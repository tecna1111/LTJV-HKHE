# CollabSphere (COSRE)

COSRE là nền tảng hỗ trợ học tập theo dự án (Project-Based Learning), được xây dựng với Spring Boot và React. Phiên bản hiện tại cung cấp luồng đăng nhập JWT, phân quyền, Dashboard và quản lý tài khoản dành cho Admin.

## Chức năng hiện có

- Đăng nhập bằng username và password.
- Xác thực stateless bằng JSON Web Token (JWT).
- Phân quyền theo vai trò.
- Tự động tạo tài khoản Admin trong môi trường development.
- Dashboard được bảo vệ và xác minh lại phiên đăng nhập qua API `/auth/me`.
- Admin tạo, cập nhật, khóa/mở khóa và xóa tài khoản.
- Giao diện đăng nhập và Dashboard responsive.
- Hỗ trợ H2 cho development và MySQL cho môi trường tích hợp.

## Vai trò người dùng

| Vai trò | Mã role | Quyền hiện tại |
|---|---|---|
| Quản trị viên | `ADMIN` | Dashboard và quản lý tài khoản |
| Trưởng bộ môn | `HEAD_DEPT` | Dashboard cơ bản |
| Nhân viên | `STAFF` | Dashboard cơ bản |
| Giảng viên | `LECTURER` | Dashboard cơ bản |
| Sinh viên | `STUDENT` | Dashboard cơ bản |

## Công nghệ

### Backend

- Java 17
- Spring Boot 4.1
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Jakarta Validation
- JWT (`jjwt` 0.11.5)
- H2 / MySQL
- Maven

### Frontend

- React 19
- Vite 8
- React Router
- Zustand
- Axios
- Lucide React
- ESLint

## Cấu trúc project

```text
LTJV-HKHE/
|-- cosre-backend/          Spring Boot REST API
|   `-- src/main/
|       |-- java/com/cosre/cosre_backend/
|       |   |-- common/     Response, constants và exception dùng chung
|       |   |-- config/     Security, JWT, CORS và seed dữ liệu
|       |   `-- modules/    Các module nghiệp vụ
|       `-- resources/      Cấu hình theo Spring profile
|-- cosre-frontend/         React + Vite SPA
|   `-- src/
|       |-- config/         Axios và cấu hình API
|       |-- routes/         Router và route bảo vệ
|       |-- store/          Trạng thái đăng nhập
|       `-- modules/        Authentication, Dashboard, Account
|-- docs/                   Tài liệu kỹ thuật
`-- CONTRIBUTING.md         Hướng dẫn chạy và đóng góp
```

## Chạy nhanh trên Windows

### 1. Chạy Backend

Mở PowerShell thứ nhất:

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-backend
.\mvnw.cmd spring-boot:run
```

Nếu Maven Wrapper không chạy:

```powershell
mvn spring-boot:run
```

### 2. Chạy Frontend

Mở PowerShell thứ hai:

```powershell
cd D:\JAVA\LTJV-HKHE\cosre-frontend
npm.cmd install
npm.cmd run dev
```

Truy cập URL được Vite hiển thị, thông thường là:

```text
http://localhost:5173/login
```

Nếu port `5173` đang được sử dụng, Vite có thể tự chuyển sang `5174`, `5175`,... Backend development đã cho phép các port localhost này.

## Tài khoản development

Profile mặc định `dev` tự động tạo tài khoản:

```text
Vai trò:  ADMIN
Username: admin
Password: Admin@123
```

Chỉ sử dụng tài khoản này khi phát triển trên máy local.

## Địa chỉ dịch vụ

| Dịch vụ | URL |
|---|---|
| Frontend | `http://localhost:5173` hoặc port Vite hiển thị |
| Backend | `http://localhost:8080` |
| API Login | `POST http://localhost:8080/api/v1/auth/login` |
| API người dùng hiện tại | `GET http://localhost:8080/api/v1/auth/me` |
| H2 Console | `http://localhost:8080/h2-console` |

## Luồng đăng nhập

```text
Login Page
   -> POST /api/v1/auth/login
   -> Backend kiểm tra tài khoản và BCrypt password
   -> Trả JWT, username, fullName và role
   -> Frontend lưu phiên đăng nhập
   -> Điều hướng /dashboard
   -> GET /api/v1/auth/me để xác minh JWT
```

- Đăng nhập thành công trả HTTP `200`.
- Sai username/password hoặc tài khoản bị khóa trả HTTP `401`.
- API được bảo vệ yêu cầu header `Authorization: Bearer <token>`.
- Chọn “Remember me” lưu phiên trong `localStorage`; bỏ chọn sử dụng `sessionStorage`.

## Kiểm tra source

Backend:

```powershell
cd cosre-backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd cosre-frontend
npm.cmd run lint
npm.cmd run build
```

## Cấu hình MySQL

Xem hướng dẫn đầy đủ trong [CONTRIBUTING.md](CONTRIBUTING.md#chạy-backend-với-mysql).

Các biến môi trường chính:

```text
SPRING_PROFILES_ACTIVE=mysql
DB_URL=jdbc:mysql://localhost:3306/cosre?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your-secure-secret-at-least-32-characters
CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:5173
```

Không commit password, JWT secret hoặc file `.env` lên Git.

## Tài liệu

- [Hướng dẫn chạy và đóng góp](CONTRIBUTING.md)
- [Tổng quan module](docs/module-overview.md)
- [Cấu trúc chuẩn hóa](docs/standardized-structure.md)

## Trạng thái

Dự án đang trong giai đoạn phát triển. Các module quản lý dự án, lớp học, Kanban, đánh giá, cộng tác thời gian thực và AI sẽ được bổ sung ở các giai đoạn tiếp theo.
