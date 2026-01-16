export interface User {
  id: number;
  username: string;
  email?: string;
  phoneNumber?: string;
  profileImage?: string;
  createdAt: string;
  isAnonymous: boolean;
  ghostMode: boolean;
  preferredLanguage?: string;
  autoTranslate: boolean;
}

export interface LoginRequest {
  email?: string;
  username?: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface RegisterRequest {
  username: string;
  email?: string;
  password: string;
  phoneNumber?: string;
}

export interface AnonymousRegisterRequest {
  username: string;
}

export interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}
