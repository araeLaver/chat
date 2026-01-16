import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '../stores/authStore';
import { showError } from '../stores/notificationStore';

const API_BASE_URL = import.meta.env.VITE_API_URL || '';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - JWT 토큰 첨부
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().token;
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - 에러 처리
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string; error?: string }>) => {
    const status = error.response?.status;

    // 401 Unauthorized - 로그아웃
    if (status === 401) {
      const authStore = useAuthStore.getState();
      if (authStore.isAuthenticated) {
        authStore.logout();
        showError('세션이 만료되었습니다. 다시 로그인해주세요.');
      }
    }

    // 403 Forbidden
    if (status === 403) {
      showError('권한이 없습니다.');
    }

    // 429 Too Many Requests
    if (status === 429) {
      showError('요청이 너무 많습니다. 잠시 후 다시 시도해주세요.');
    }

    // 500+ Server Error
    if (status && status >= 500) {
      showError('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    }

    // 네트워크 에러
    if (!error.response) {
      showError('네트워크 연결을 확인해주세요.');
    }

    return Promise.reject(error);
  }
);

export default apiClient;
