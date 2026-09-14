/**
 * apiClient.js
 * -------------------------------------------------------------------------
 * Axios instance dùng chung cho toàn bộ cosre-frontend.
 * Nếu project đã có sẵn 1 file tương tự (ví dụ trong src/services/http.js
 * do Công Duy hoặc bạn khác tạo), dùng file đó thay thế và xóa file này để
 * tránh có 2 client khác nhau.
 *
 * Base URL đọc từ biến môi trường VITE_API_BASE_URL (khai báo trong file
 * .env ở thư mục cosre-frontend), fallback về localhost:8080/api nếu chưa
 * cấu hình (cổng mặc định của Spring Boot).
 * -------------------------------------------------------------------------
 */

import axios from "axios";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// Tự động đính kèm JWT token (nếu có) vào mỗi request
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Xử lý lỗi 401 tập trung (token hết hạn / chưa đăng nhập)
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // TODO: điều hướng về trang login khi tích hợp router thật
      console.warn("Phiên đăng nhập đã hết hạn hoặc chưa đăng nhập.");
    }
    return Promise.reject(error);
  }
);

export default apiClient;
