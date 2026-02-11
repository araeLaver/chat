import apiClient from './client';
import { Room, CreateRoomRequest, UpdateRoomRequest, RoomMember } from '../types/room';
import { RoomMessage, SendMessageRequest } from '../types/chat';

export const roomsApi = {
  getMyRooms() {
    return apiClient.get<Room[]>('/rooms/my-rooms');
  },

  createRoom(data: CreateRoomRequest) {
    return apiClient.post<Room>('/rooms', data);
  },

  updateRoom(roomId: number, data: UpdateRoomRequest) {
    return apiClient.put(`/rooms/${roomId}`, data);
  },

  deleteRoom(roomId: number) {
    return apiClient.delete(`/rooms/${roomId}`);
  },

  getMessages(roomId: string | number) {
    return apiClient.get<RoomMessage[]>(`/rooms/${roomId}/messages`);
  },

  sendMessage(roomId: string | number, data: SendMessageRequest) {
    return apiClient.post(`/rooms/${roomId}/messages`, data);
  },

  getMembers(roomId: string | number) {
    return apiClient.get<RoomMember[]>(`/rooms/${roomId}/members`);
  },

  addMember(roomId: string | number, userId: number) {
    return apiClient.post(`/rooms/${roomId}/members`, { userId });
  },

  removeMember(roomId: string | number, userId: number) {
    return apiClient.delete(`/rooms/${roomId}/members/${userId}`);
  },

  leaveRoom(roomId: string | number) {
    return apiClient.post(`/rooms/${roomId}/leave`);
  },

  markAsRead(roomId: string | number) {
    return apiClient.post(`/rooms/${roomId}/read`);
  },

  searchRooms(keyword: string) {
    return apiClient.get<Room[]>('/rooms/search', { params: { keyword } });
  },
};
