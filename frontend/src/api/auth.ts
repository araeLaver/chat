import apiClient from './client';
import { AuthRequest, AuthResponse, GuestResponse } from '../types/auth';

export const authApi = {
  login(data: { username: string; password: string }) {
    return apiClient.post<AuthResponse>('/auth/login', data);
  },

  register(data: AuthRequest) {
    return apiClient.post<AuthResponse>('/auth/register', data);
  },

  guestLogin() {
    return apiClient.post<GuestResponse>('/auth/guest');
  },

  logout() {
    return apiClient.post('/auth/logout');
  },
};
