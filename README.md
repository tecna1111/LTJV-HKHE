# 🌐 CollabSphere (COSRE)
**Hệ thống hỗ trợ việc học theo phương pháp học tập dựa trên dự án (Project-Based Learning)**

![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2%2B-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Status](https://img.shields.io/badge/Status-Active_Development-yellow?style=for-the-badge)



## Tính Năng Nổi Bật

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

## Vai Trò Người Dùng

| Vai trò | Phân quyền chính |
|---|---|
| **Quản trị viên** | Quản lý toàn bộ tài khoản hệ thống, xử lý báo cáo, theo dõi Dashboard. |
| **Nhân viên** | Import dữ liệu môn học, lớp học, tài khoản; phân công giảng viên & sinh viên. |
| **Trưởng bộ môn** | Duyệt/Từ chối dự án, phân bổ kho dự án chuẩn cho các lớp. |
| **Giảng viên** | Tạo dự án, quản lý nhóm, theo dõi tiến độ, chấm điểm, đánh giá và hỗ trợ sinh viên. |
| **Sinh viên** | Quản lý task, nộp bài, cộng tác thời gian thực, đánh giá chéo và sử dụng AI hỗ trợ. |

---

### ⚙️ Cài Đặt (Backend Java - Spring Boot)
```
# 1. Clone dự án
git clone https://github.com/tecna1111/LTJV-HKHE
cd LTJV-HKHE

# 2. Cấu hình Database
# Mở file src/main/resources/application.yml và cập nhật thông tin MySQL của bạn:
# spring.datasource.url=jdbc:mysql://localhost:3306/collabsphere
# spring.datasource.username=root
# spring.datasource.password=yourpassword

# 3. Build dự án với Maven
mvn clean install

# 4. Chạy ứng dụng
mvn spring-boot:run

### 📂 Cấu Trúc Dự Án 
CollabSphere/
├── backend/                  # Spring Boot (Java)
│   ├── src/main/java/
│   │   └── com/cosre/
│   │       ├── config/       # Cấu hình Security, WebSocket, AI
│   │       ├── controllers/  # API Endpoints
│   │       ├── models/       # Entities (User, Project, Task,...)
│   │       ├── repositories/ # Database Access
│   │       └── services/     # Business Logic
│   └── pom.xml
├── frontend/                 # Tương tác giao diện người dùng
├── ai-service/               # (Tùy chọn) Microservice xử lý AI Python/Java
└── docs/                     # Tài liệu thiết kế, sơ đồ, UML
