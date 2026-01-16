import apiClient from './axios';
import type { Friend, FriendRequest, UserSearchResult } from '../types';

export const friendsApi = {
  // 친구 목록
  getFriends: async (): Promise<Friend[]> => {
    const response = await apiClient.get<Friend[]>('/api/friends/list');
    return response.data;
  },

  // 친구 요청 보내기
  sendFriendRequest: async (userId: number): Promise<void> => {
    await apiClient.post('/api/friends/request', { targetUserId: userId });
  },

  // 받은 친구 요청 목록
  getReceivedRequests: async (): Promise<FriendRequest[]> => {
    const response = await apiClient.get<FriendRequest[]>('/api/friends/requests/received');
    return response.data;
  },

  // 보낸 친구 요청 목록
  getSentRequests: async (): Promise<FriendRequest[]> => {
    const response = await apiClient.get<FriendRequest[]>('/api/friends/requests/sent');
    return response.data;
  },

  // 친구 요청 개수 조회
  getRequestCount: async (): Promise<{ count: number }> => {
    const response = await apiClient.get<{ count: number }>('/api/friends/requests/count');
    return response.data;
  },

  // 친구 요청 수락
  acceptFriendRequest: async (requestId: number): Promise<void> => {
    await apiClient.post('/api/friends/accept', { requestId });
  },

  // 친구 요청 거절
  rejectFriendRequest: async (requestId: number): Promise<void> => {
    await apiClient.post('/api/friends/reject', { requestId });
  },

  // 친구 삭제
  removeFriend: async (friendId: number): Promise<void> => {
    await apiClient.delete(`/api/friends/unfriend/${friendId}`);
  },

  // 친구 차단
  blockFriend: async (userId: number): Promise<void> => {
    await apiClient.post('/api/friends/block', { targetUserId: userId });
  },

  // 사용자 검색
  searchUsers: async (query: string): Promise<UserSearchResult[]> => {
    const response = await apiClient.get<UserSearchResult[]>('/api/friends/search', {
      params: { keyword: query },
    });
    return response.data;
  },
};
