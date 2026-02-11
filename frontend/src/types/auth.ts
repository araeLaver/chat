export interface User {
  id: number;
  username: string;
  displayName: string;
  phoneNumber?: string;
  email?: string;
  profileImageUrl?: string;
  statusMessage?: string;
}

export interface AuthRequest {
  username: string;
  password: string;
  phoneNumber?: string;
  displayName?: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  userId: number;
  displayName: string;
  phoneNumber?: string;
  message: string;
}

export interface GuestResponse {
  success: boolean;
  message: string;
  token: string;
  user: {
    id: number;
    username: string;
    displayName: string;
  };
  defaultRoomId?: number;
}
