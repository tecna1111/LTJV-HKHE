Markdown
# 🌐 CollabSphere (COSRE)
**Hệ thống hỗ trợ việc học theo phương pháp học tập dựa trên dự án (Project-Based Learning)**

![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2%2B-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Status](https://img.shields.io/badge/Status-Active_Development-yellow?style=for-the-badge)

## ✨ Tính Năng Nổi Bật

### Dành cho Quản lý & Học thuật
- **Quản lý toàn diện:** Tự động hóa tạo tài khoản, lớp học, môn học qua file nhập (Import).
- **Quản lý dự án đa cấp:** Từ Trưởng bộ môn (phê duyệt) đến Giảng viên (tạo, phân công, giám sát).
- **Hệ thống đánh giá đa chiều:** Giảng viên đánh giá nhóm/cá nhân, sinh viên đánh giá chéo (peer-review) theo từng mốc dự án.

### Cộng tác Thời gian thực
- **Workspace Tích hợp:** Quản lý task, Kanban board, thẻ và nhiệm vụ phụ trong từng Sprint.
- **Giao tiếp Trực tiếp:** Chat nhóm thời gian thực, họp Video/Audio chất lượng cao có chia sẻ màn hình.
- **Công cụ Sáng tạo:** Bảng trắng (Whiteboard) và trình soạn thảo văn bản đồng bộ thời gian thực cho nhiều người dùng.

### Tích hợp AI Trợ lý
- **Tự động hóa:** Tạo thông tin dự án, mục tiêu và mốc thời gian tự động.
- **Chatbot thông minh:** Hỗ trợ sinh viên động não ý tưởng, giải quyết vấn đề.
- **Phân tích dữ liệu:** Đưa ra tư vấn và đề xuất cho Giảng viên dựa trên mức độ đóng góp và tiến độ của sinh viên.

---

## 👥 Vai Trò Người Dùng

| Vai trò | Phân quyền chính |
|---|---|
| **Quản trị viên** | Quản lý toàn bộ tài khoản hệ thống, xử lý báo cáo, theo dõi Dashboard. |
| **Nhân viên** | Import dữ liệu môn học, lớp học, tài khoản; phân công giảng viên & sinh viên. |
| **Trưởng bộ môn** | Duyệt/Từ chối dự án, phân bổ kho dự án chuẩn cho các lớp. |
| **Giảng viên** | Tạo dự án, quản lý nhóm, theo dõi tiến độ, chấm điểm, đánh giá và hỗ trợ sinh viên. |
| **Sinh viên** | Quản lý task, nộp bài, cộng tác thời gian thực, đánh giá chéo và sử dụng AI hỗ trợ. |

---

## 📂 Cấu Trúc Dự Án

```text
LTJV-HKHE/
├── docs/                                  # Tài liệu thiết kế, báo cáo, sơ đồ UML (Tuần 1)
│
├── Cosre_Backend/                         # Spring Boot Application
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── src/main/
│       ├── java/collabsphere/
│       │   ├── CollabSphereApplication.java
│       │   ├── config/                    # Security, WebSocket, Async Config
│       │   ├── controller/                # REST API Controllers
│       │   ├── dto/                       # Request & Response DTOs
│       │   ├── entity/                    # Database Entities (JPA / Hibernate)
│       │   ├── enums/                     # Status & Role Enums
│       │   ├── exception/                 # Global Exception Handling
│       │   ├── repository/                # JPA Repositories
│       │   ├── security/                  # JWT Authentication
│       │   ├── service/                   # Business Logic & AI
│       │   └── websocket/                 # Real-time Event Handlers
│       └── resources/                     # application.yml, Email Templates
│
├── Cosre_Frontend/                        # React + Vite Application
│   ├── eslint.config.js
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   ├── public/
│   └── src/
│       ├── App.jsx                        # Router & Main Routes
│       ├── main.jsx                       # Entry Point
│       ├── api/                           # Axios Services
│       ├── assets/                        # Static Images & Icons
│       ├── components/                    # Reusable UI & Widgets
│       ├── context/                       # React Contexts (Auth, Socket, Workspace)
│       ├── hooks/                         # Custom Hooks
│       ├── pages/                         # Pages grouped by Roles
│       └── utils/                         # Helper functions & constants
│
├── .gitignore
└── README.md


⚙️ Hướng Dẫn Cài Đặt & Chạy Dự Án
Backend (Spring Boot)
Bash
# 1. Clone dự án về máy
git clone [https://github.com/tecna1111/LTJV-HKHE](https://github.com/tecna1111/LTJV-HKHE)
cd LTJV-HKHE/Cosre_Backend

# 2. Cấu hình Database
# Mở file src/main/resources/application.yml và cập nhật thông tin MySQL

# 3. Build và Chạy ứng dụng
mvn clean install
mvn spring-boot:run

Frontend (React + Vite)

Bash
# 1. Chuyển sang thư mục Frontend
cd LTJV-HKHE/Cosre_Frontend

# 2. Cài đặt các thư viện phụ thuộc
npm install

# 3. Chạy môi trường Development
npm run dev

📌 Quy Tắc Viết Commit Message (Conventional Commits)
Plaintext
<type>(<scope>): <mô tả ngắn gọn>
feat: Thêm tính năng mới (vd: feat(auth): thêm API đăng nhập JWT)

fix: Sửa lỗi (vd: fix(chat): sửa lỗi mất kết nối websocket)

docs: Tài liệu/README (vd: docs(srs): cập nhật sơ đồ Use Case)

style: Định dạng CSS/UI (vd: style(sidebar): chỉnh lại padding)

refactor: Tối ưu hóa code (vd: refactor(service): tối ưu truy vấn JPA)

chore: Cấu hình/Thư viện (vd: chore: cài đặt thư viện TailwindCSS)