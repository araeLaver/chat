import apiClient from './axios';
import type { ChatRoom, CreateRoomRequest, RoomUser } from '../types';

export const roomsApi = {
  // 참여 중인 채팅방 목록
  getMyRooms: async (): Promise<ChatRoom[]> => {
    const response = await apiClient.get<ChatRoom[]>('/api/rooms/my-rooms');
    return response.data;
  },

  // 채팅방 검색
  searchRooms: async (query: string): Promise<ChatRoom[]> => {
    const response = await apiClient.get<ChatRoom[]>('/api/rooms/search', {
      params: { keyword: query },
    });
    return response.data;
  },

  // 채팅방 상세 정보
  getRoom: async (roomId: string): Promise<ChatRoom> => {
    const response = await apiClient.get<ChatRoom>(`/api/rooms/${roomId}`);
    return response.data;
  },

  // 채팅방 생성
  createRoom: async (data: CreateRoomRequest): Promise<ChatRoom> => {
    const response = await apiClient.post<ChatRoom>('/api/rooms', data);
    return response.data;
  },

  // 채팅방 수정
  updateRoom: async (roomId: string, data: Partial<CreateRoomRequest>): Promise<ChatRoom> => {
    const response = await apiClient.put<ChatRoom>(`/api/rooms/${roomId}`, data);
    return response.data;
  },

  // 채팅방 나가기
  leaveRoom: async (roomId: string): Promise<void> => {
    await apiClient.post(`/api/rooms/${roomId}/leave`);
  },

  // 채팅방 삭제 (방장만)
  deleteRoom: async (roomId: string): Promise<void> => {
    await apiClient.delete(`/api/rooms/${roomId}`);
  },

  // 멤버 추가
  addMember: async (roomId: string, userId: number): Promise<void> => {
    await apiClient.post(`/api/rooms/${roomId}/members`, { userId });
  },

  // 멤버 제거
  removeMember: async (roomId: string, userId: number): Promise<void> => {
    await apiClient.delete(`/api/rooms/${roomId}/members/${userId}`);
  },

  // 멤버 목록
  getMembers: async (roomId: string): Promise<RoomUser[]> => {
    const response = await apiClient.get<RoomUser[]>(`/api/rooms/${roomId}/members`);
    return response.data;
  },
};
