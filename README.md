# 🌐 CollabSphere (COSRE)
**Hệ thống hỗ trợ việc học theo phương pháp học tập dựa trên dự án (Project-Based Learning)**

![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2%2B-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Status](https://img.shields.io/badge/Status-Active_Development-yellow?style=for-the-badge)

---

## 📖 Bối Cảnh & Giải Pháp

Trong giáo dục hiện đại, **Học tập Dựa trên Dự án (PBL)** là phương pháp cốt lõi giúp sinh viên phát triển kỹ năng thực tiễn. Tuy nhiên, việc phải sử dụng quá nhiều nền tảng rời rạc (chat, họp video, quản lý task, bảng trắng...) làm giảm hiệu suất làm việc nhóm và khó khăn trong việc theo dõi, đánh giá của giảng viên.

**CollabSphere (COSRE)** ra đời nhằm cung cấp một nền tảng quản lý dự án và cộng tác thời gian thực **tất-cả-trong-một**. Hệ thống hợp nhất giao tiếp, quản lý không gian làm việc, bảng trắng, hệ thống đánh giá chéo và tích hợp AI, giúp tạo ra một môi trường học tập thực hành liền mạch và hiệu quả.

---

## ✨ Tính Năng Nổi Bật

### 🛠 Dành cho Quản lý & Học thuật
- **Quản lý toàn diện:** Tự động hóa tạo tài khoản, lớp học, môn học qua file nhập (Import).
- **Quản lý dự án đa cấp:** Từ Trưởng bộ môn (phê duyệt) đến Giảng viên (tạo, phân công, giám sát).
- **Hệ thống đánh giá đa chiều:** Giảng viên đánh giá nhóm/cá nhân, sinh viên đánh giá chéo (peer-review) theo từng mốc dự án.

### 🤝 Cộng tác Thời gian thực
- **Workspace Tích hợp:** Quản lý task, Kanban board, thẻ và nhiệm vụ phụ trong từng Sprint.
- **Giao tiếp Trực tiếp:** Chat nhóm thời gian thực, họp Video/Audio chất lượng cao có chia sẻ màn hình.
- **Công cụ Sáng tạo:** Bảng trắng (Whiteboard) và trình soạn thảo văn bản đồng bộ thời gian thực cho nhiều người dùng.

### 🤖 Tích hợp AI Trợ lý
- **Tự động hóa:** Tạo thông tin dự án, mục tiêu và mốc thời gian tự động.
- **Chatbot thông minh:** Hỗ trợ sinh viên động não ý tưởng, giải quyết vấn đề.
- **Phân tích dữ liệu:** Đưa ra tư vấn và đề xuất cho Giảng viên dựa trên mức độ đóng góp và tiến độ của sinh viên.

---

## 👥 Vai Trò Người Dùng

| Vai trò | Phân quyền chính |
|---|---|
| 👑 **Quản trị viên** | Quản lý toàn bộ tài khoản hệ thống, xử lý báo cáo, theo dõi Dashboard. |
| 💼 **Nhân viên** | Import dữ liệu môn học, lớp học, tài khoản; phân công giảng viên & sinh viên. |
| 🎓 **Trưởng bộ môn** | Duyệt/Từ chối dự án, phân bổ kho dự án chuẩn cho các lớp. |
| 👨‍🏫 **Giảng viên** | Tạo dự án, quản lý nhóm, theo dõi tiến độ, chấm điểm, đánh giá và hỗ trợ sinh viên. |
| 👨‍💻 **Sinh viên** | Quản lý task, nộp bài, cộng tác thời gian thực, đánh giá chéo và sử dụng AI hỗ trợ. |

---

##  Bắt Đầu Nhanh

### Yêu Cầu Hệ Thống
- **JDK:** 17 hoặc 21+
- **Maven:** 3.8+
- **Database:** MySQL 8.0+
- **Node.js:** (Cho Frontend - nếu có)

### ⚙️ Cài Đặt (Backend Java - Spring Boot)

```bash
# 1. Clone dự án
git clone [https://github.com/YourUsername/CollabSphere.git](https://github.com/YourUsername/CollabSphere.git)
cd CollabSphere/backend

# 2. Cấu hình Database
# Mở file src/main/resources/application.yml và cập nhật thông tin MySQL của bạn:
# spring.datasource.url=jdbc:mysql://localhost:3306/collabsphere
# spring.datasource.username=root
# spring.datasource.password=yourpassword

# 3. Build dự án với Maven
mvn clean install

# 4. Chạy ứng dụng
mvn spring-boot:run
