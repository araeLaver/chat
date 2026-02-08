import apiClient from './axios';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  AnonymousRegisterRequest,
  User,
} from '../types';

export const authApi = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/v1/auth/login', data);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/v1/auth/register', data);
    return response.data;
  },

  registerAnonymous: async (
    data: AnonymousRegisterRequest,
  ): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>(
      '/api/v1/auth/register/anonymous',
      data,
    );
    return response.data;
  },

  getProfile: async (): Promise<User> => {
    const response = await apiClient.get<User>('/api/v1/auth/profile');
    return response.data;
  },

  updateProfile: async (data: Partial<User>): Promise<User> => {
    const response = await apiClient.put<User>('/api/v1/auth/profile', data);
    return response.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/api/v1/auth/logout');
  },
};

export default authApi;
