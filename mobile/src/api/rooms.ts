import apiClient from './axios';
import type {ChatRoom, CreateRoomRequest, RoomListResponse} from '../types';

export const roomsApi = {
  getRooms: async (): Promise<RoomListResponse> => {
    const response = await apiClient.get<RoomListResponse>('/api/v1/rooms');
    return response.data;
  },

  getRoom: async (roomId: string): Promise<ChatRoom> => {
    const response = await apiClient.get<ChatRoom>(`/api/v1/rooms/${roomId}`);
    return response.data;
  },

  createRoom: async (data: CreateRoomRequest): Promise<ChatRoom> => {
    const response = await apiClient.post<ChatRoom>('/api/v1/rooms', data);
    return response.data;
  },

  joinRoom: async (roomId: string): Promise<void> => {
    await apiClient.post(`/api/v1/rooms/${roomId}/join`);
  },

  leaveRoom: async (roomId: string): Promise<void> => {
    await apiClient.post(`/api/v1/rooms/${roomId}/leave`);
  },

  deleteRoom: async (roomId: string): Promise<void> => {
    await apiClient.delete(`/api/v1/rooms/${roomId}`);
  },
};

export default roomsApi;
