import apiClient from './axios';
import type { User, LoginRequest, LoginResponse, RegisterRequest, AnonymousRegisterRequest } from '../types';

export const authApi = {
  // 회원가입
  register: async (data: RegisterRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/register', data);
    return response.data;
  },

  // 익명 가입
  registerAnonymous: async (data: AnonymousRegisterRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/register/anonymous', data);
    return response.data;
  },

  // 로그인
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/login', data);
    return response.data;
  },

  // 현재 사용자 정보
  getCurrentUser: async (): Promise<User> => {
    const response = await apiClient.get<User>('/api/users/me');
    return response.data;
  },

  // 프로필 업데이트
  updateProfile: async (data: Partial<User>): Promise<User> => {
    const response = await apiClient.put<User>('/api/users/me', data);
    return response.data;
  },

  // 비밀번호 변경
  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await apiClient.put('/api/users/me/password', {
      currentPassword,
      newPassword,
    });
  },

  // 프라이버시 설정 조회
  getPrivacySettings: async (): Promise<{ ghostMode: boolean; disableIpLogging: boolean }> => {
    const response = await apiClient.get('/api/privacy/settings');
    return response.data;
  },

  // 프라이버시 설정 업데이트
  updatePrivacySettings: async (data: { ghostMode?: boolean; disableIpLogging?: boolean }): Promise<void> => {
    await apiClient.put('/api/privacy/settings', data);
  },

  // 번역 설정 조회
  getTranslationSettings: async () => {
    const response = await apiClient.get('/api/translate/settings');
    return response.data;
  },

  // 번역 설정 업데이트
  updateTranslationSettings: async (data: { autoTranslate?: boolean; preferredLanguage?: string }): Promise<void> => {
    await apiClient.put('/api/translate/settings', data);
  },

  // 로그아웃 (토큰 무효화)
  logout: async (): Promise<void> => {
    try {
      await apiClient.post('/api/auth/logout');
    } catch {
      // 로그아웃 실패해도 로컬에서 처리
    }
  },

  // 토큰 갱신
  refreshToken: async (): Promise<{ token: string }> => {
    const response = await apiClient.post('/api/auth/refresh');
    return response.data;
  },
};
