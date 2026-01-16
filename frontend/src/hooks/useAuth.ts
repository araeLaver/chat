import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { authApi } from '../api';
import { showSuccess, showError } from '../stores/notificationStore';
import type { LoginRequest, RegisterRequest, AnonymousRegisterRequest } from '../types';

export function useAuth() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);

  const {
    token,
    user,
    isAuthenticated,
    login: storeLogin,
    logout: storeLogout,
    updateUser,
    setError,
  } = useAuthStore();

  // 로그인
  const login = useCallback(async (data: LoginRequest) => {
    setIsLoading(true);
    try {
      const response = await authApi.login(data);
      storeLogin(response.token, response.user);
      showSuccess('로그인되었습니다.');
      navigate('/chat');
    } catch (error: any) {
      const message = error.response?.data?.message || '로그인에 실패했습니다.';
      setError(message);
      showError(message);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [storeLogin, navigate, setError]);

  // 회원가입
  const register = useCallback(async (data: RegisterRequest) => {
    setIsLoading(true);
    try {
      const response = await authApi.register(data);
      storeLogin(response.token, response.user);
      showSuccess('회원가입이 완료되었습니다.');
      navigate('/chat');
    } catch (error: any) {
      const message = error.response?.data?.message || '회원가입에 실패했습니다.';
      setError(message);
      showError(message);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [storeLogin, navigate, setError]);

  // 익명 가입
  const registerAnonymous = useCallback(async (data: AnonymousRegisterRequest) => {
    setIsLoading(true);
    try {
      const response = await authApi.registerAnonymous(data);
      storeLogin(response.token, response.user);
      showSuccess('익명으로 로그인되었습니다.');
      navigate('/chat');
    } catch (error: any) {
      const message = error.response?.data?.message || '익명 로그인에 실패했습니다.';
      setError(message);
      showError(message);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [storeLogin, navigate, setError]);

  // 로그아웃
  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // 서버 로그아웃 실패해도 로컬에서 처리
    } finally {
      storeLogout();
      showSuccess('로그아웃되었습니다.');
      navigate('/login');
    }
  }, [storeLogout, navigate]);

  // 프로필 업데이트
  const updateProfile = useCallback(async (data: Partial<NonNullable<typeof user>>) => {
    setIsLoading(true);
    try {
      const updatedUser = await authApi.updateProfile(data);
      updateUser(updatedUser);
      showSuccess('프로필이 업데이트되었습니다.');
    } catch (error: any) {
      const message = error.response?.data?.message || '프로필 업데이트에 실패했습니다.';
      showError(message);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [updateUser]);

  // 현재 사용자 정보 새로고침
  const refreshUser = useCallback(async () => {
    if (!token) return;

    try {
      const currentUser = await authApi.getCurrentUser();
      updateUser(currentUser);
    } catch (error) {
      console.error('Failed to refresh user:', error);
    }
  }, [token, updateUser]);

  return {
    user,
    token,
    isAuthenticated,
    isLoading,
    login,
    register,
    registerAnonymous,
    logout,
    updateProfile,
    refreshUser,
  };
}
