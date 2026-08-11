# Module Overview: Authentication & Account Management

## Mục tiêu chung
Module này xây dựng phần cốt lõi cho tuần 1:
- Đăng nhập bằng JWT
- Quản lý người dùng (Admin CRUD)
- Bảo mật route backend
- Frontend Login và trang quản lý user cơ bản

## Backend
### Các chức năng chính
- `common/constants/RoleEnum.java`: khai báo vai trò (`ADMIN`, `HEAD_DEPT`, `STAFF`, `LECTURER`, `STUDENT`)
- `common/dto/ApiResponse.java`: chuẩn hóa phản hồi JSON cho frontend
- `config/JwtConfig.java`: tạo và xác thực token JWT
- `config/SecurityConfig.java`: cấu hình CORS, bảo mật route và dùng JWT filter
- `modules/account/entity/User.java`: định nghĩa thực thể người dùng
- `modules/account/repository/UserRepository.java`: truy vấn user theo `username` hoặc `email`
- `modules/account/service/AccountService.java`: logic tạo, cập nhật, xóa, bật/tắt tài khoản
- `modules/account/controller/AccountController.java`: API quản lý user (`/api/v1/accounts`)
- `modules/authentication/dto/LoginRequest.java` và `LoginResponse.java`
- `modules/authentication/service/AuthService.java`: xử lý kiểm tra đăng nhập và trả JWT
- `modules/authentication/controller/AuthController.java`: endpoint `/api/v1/auth/login`

### API Endpoints
- `POST /api/v1/auth/login`
- `GET /api/v1/accounts`
- `POST /api/v1/accounts`
- `PUT /api/v1/accounts/{id}`
- `DELETE /api/v1/accounts/{id}`
- `PUT /api/v1/accounts/{id}/status?active=true|false`

### Cấu hình chạy backend
- `application.properties` dùng H2 in-memory database
- JWT secret và expiration đã cấu hình
- Server chạy tại `http://localhost:8080`

## Frontend
### Các phần chính
- `src/config/axios.js`: cấu hình axios và tự động thêm token
- `src/store/useAuthStore.js`: lưu token, username, role bằng Zustand
- `src/routes/AppRoutes.jsx`: định tuyến ứng dụng
- `src/routes/ProtectedRoute.jsx`: bảo vệ route, kiểm tra login và role
- `src/modules/authentication/authService.js`: gọi API login
- `src/modules/authentication/pages/LoginPage.jsx`: trang đăng nhập
- `src/modules/account/accountService.js`: gọi API quản lý user
- `src/modules/account/pages/UserManagementPage.jsx`: trang quản lý người dùng của Admin

### Chạy frontend
- `cd cosre-frontend`
- `npm install`
- `npm run dev`

## Lộ trình tiếp theo
1. Hoàn thiện `dashboard` và route chính cho từng role.
2. Thêm validation đầu vào và xử lý lỗi chi tiết.
3. Kết nối database thực tế (MySQL) khi cần.
4. Mở rộng API `account` với phân quyền sâu hơn và tìm kiếm / phân trang.
5. Xây dựng UI `dashboard` cho `ADMIN`, `LECTURER`, `STUDENT`.

## Ghi chú
- Backend hiện dùng H2 để chạy nhanh, nếu muốn dùng MySQL cần thay `application.properties`.
- Các route frontend hiện tại giả định `ADMIN` mới truy cập được trang quản lý user.
- File này là tài liệu tổng quan module đang xây dựng để các thành viên khác dễ nắm bắt.
