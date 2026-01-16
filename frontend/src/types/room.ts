import type { User } from './auth';

export interface ChatRoom {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  creatorId?: number;
  isPrivate: boolean;
  maxUsers?: number;
  users: RoomUser[];
  unreadCount?: number;
  lastMessage?: string;
  lastMessageTime?: string;
}

export interface RoomUser {
  id: string;
  username: string;
  sessionId: string;
  userId?: number;
  joinTime: number;
  isOnline?: boolean;
}

export interface CreateRoomRequest {
  roomName: string;
  description?: string;
  isPrivate?: boolean;
  maxUsers?: number;
}

export interface JoinRoomRequest {
  roomId: string;
  username: string;
  userId?: number;
}

export interface RoomListResponse {
  rooms: ChatRoom[];
  totalCount: number;
}

export interface GroupRoom {
  id: number;
  name: string;
  description?: string;
  creatorId: number;
  createdAt: string;
  isActive: boolean;
  maxMembers: number;
  memberCount: number;
  members?: GroupMember[];
  unreadCount?: number;
  lastMessage?: string;
  lastMessageTime?: string;
}

export interface GroupMember {
  id: number;
  roomId: number;
  userId: number;
  username: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
  joinedAt: string;
  lastReadAt?: string;
  isMuted: boolean;
  user?: User;
}
