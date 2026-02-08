import apiClient from './axios';
import type {Friend, FriendRequest, UserSearchResult} from '../types';

export const friendsApi = {
  getFriends: async (): Promise<Friend[]> => {
    const response = await apiClient.get<Friend[]>('/api/v1/friends/list');
    return response.data;
  },

  searchUsers: async (query: string): Promise<UserSearchResult[]> => {
    const response = await apiClient.get<UserSearchResult[]>(
      '/api/v1/friends/search',
      {
        params: {query},
      },
    );
    return response.data;
  },

  sendFriendRequest: async (friendId: number): Promise<void> => {
    await apiClient.post('/api/v1/friends/request', {friendId});
  },

  acceptFriendRequest: async (requesterId: number): Promise<void> => {
    await apiClient.post('/api/v1/friends/accept', {requesterId});
  },

  rejectFriendRequest: async (requesterId: number): Promise<void> => {
    await apiClient.post('/api/v1/friends/reject', {requesterId});
  },

  unfriend: async (friendId: number): Promise<void> => {
    await apiClient.delete(`/api/v1/friends/unfriend/${friendId}`);
  },

  blockUser: async (userId: number): Promise<void> => {
    await apiClient.post('/api/v1/friends/block', {userId});
  },

  getPendingRequests: async (): Promise<FriendRequest[]> => {
    const response = await apiClient.get<FriendRequest[]>(
      '/api/v1/friends/requests/received',
    );
    return response.data;
  },

  getSentRequests: async (): Promise<FriendRequest[]> => {
    const response = await apiClient.get<FriendRequest[]>(
      '/api/v1/friends/requests/sent',
    );
    return response.data;
  },
};

export default friendsApi;
