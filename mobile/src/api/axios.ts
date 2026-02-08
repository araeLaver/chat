import axios, {
  type AxiosError,
  type InternalAxiosRequestConfig,
} from 'axios';
import {useAuthStore} from '../stores/authStore';
import {Alert} from 'react-native';

// API URL - Change this to your server URL
const API_BASE_URL = __DEV__
  ? 'http://10.0.2.2:8080' // Android emulator localhost
  : 'https://chat-untab-fddd496d.koyeb.app';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - Attach JWT token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().token;
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  },
);

// Response interceptor - Error handling
apiClient.interceptors.response.use(
  response => response,
  (error: AxiosError<{message?: string; error?: string}>) => {
    const status = error.response?.status;

    // 401 Unauthorized - Logout
    if (status === 401) {
      const authStore = useAuthStore.getState();
      if (authStore.isAuthenticated) {
        authStore.logout();
        Alert.alert('세션 만료', '세션이 만료되었습니다. 다시 로그인해주세요.');
      }
    }

    // 403 Forbidden
    if (status === 403) {
      Alert.alert('권한 없음', '권한이 없습니다.');
    }

    // 429 Too Many Requests
    if (status === 429) {
      Alert.alert(
        '요청 제한',
        '요청이 너무 많습니다. 잠시 후 다시 시도해주세요.',
      );
    }

    // 500+ Server Error
    if (status && status >= 500) {
      Alert.alert(
        '서버 오류',
        '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
      );
    }

    // Network error
    if (!error.response) {
      Alert.alert('네트워크 오류', '네트워크 연결을 확인해주세요.');
    }

    return Promise.reject(error);
  },
);

export default apiClient;
