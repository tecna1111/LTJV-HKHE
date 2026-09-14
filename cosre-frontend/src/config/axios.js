import axios from 'axios';
import { getAuthToken, getRefreshToken, replaceTokens } from '../store/useAuthStore';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = getAuthToken();
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshRequest;

const clearStoredAuth = () => {
  ['cosre_token', 'cosre_refresh_token', 'cosre_username', 'cosre_full_name', 'cosre_role'].forEach((key) => {
    localStorage.removeItem(key);
    sessionStorage.removeItem(key);
  });
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const refreshToken = getRefreshToken();

    if (error.response?.status === 401 && refreshToken && !originalRequest._retried
        && !originalRequest.url?.includes('/auth/refresh')) {
      originalRequest._retried = true;
      try {
        refreshRequest ??= axios.post(`${api.defaults.baseURL}/auth/refresh`, { refreshToken })
          .finally(() => { refreshRequest = null; });
        const response = await refreshRequest;
        replaceTokens(response.data.data.token, response.data.data.refreshToken);
        originalRequest.headers.Authorization = `Bearer ${response.data.data.token}`;
        return api(originalRequest);
      } catch (refreshError) {
        clearStoredAuth();
        if (window.location.pathname !== '/login') window.location.assign('/login');
        return Promise.reject(refreshError);
      }
    }

    if (error.response?.status === 401) {
      clearStoredAuth();
      if (window.location.pathname !== '/login') window.location.assign('/login');
    }
    return Promise.reject(error);
  },
);

export function getApiError(error, fallback = 'Đã xảy ra lỗi. Vui lòng thử lại.') {
  const response = error.response;
  if (!response && error.isAxiosError) return 'Không kết nối được máy chủ. Vui lòng kiểm tra backend và kết nối mạng.';
  if (response?.data?.message === 'Validation failed' && response.data.data) {
    return Object.values(response.data.data).join('; ');
  }
  if (!response?.data?.message && response?.status === 403)
    return 'Máy chủ từ chối yêu cầu (403). Vui lòng kiểm tra tài khoản và quyền giảng viên.';
  if (!response?.data?.message && response?.status >= 500)
    return `Máy chủ gặp lỗi (${response.status}). Vui lòng kiểm tra log backend.`;
  return error.response?.data?.message || fallback;
}

export default api;
