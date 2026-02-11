import apiClient from './client';
import { Friend, FriendRequest, UserSearchResult } from '../types/friend';

export const friendsApi = {
  getFriends() {
    return apiClient.get<Friend[]>('/friends/list');
  },

  getReceivedRequests() {
    return apiClient.get<FriendRequest[]>('/friends/requests/received');
  },

  getSentRequests() {
    return apiClient.get<FriendRequest[]>('/friends/requests/sent');
  },

  getRequestCount() {
    return apiClient.get<number>('/friends/requests/count');
  },

  sendRequest(friendId: number) {
    return apiClient.post('/friends/request', { friendId });
  },

  acceptRequest(requesterId: number) {
    return apiClient.post('/friends/accept', { requesterId });
  },

  rejectRequest(requesterId: number) {
    return apiClient.post('/friends/reject', { requesterId });
  },

  blockUser(userId: number) {
    return apiClient.post('/friends/block', { userId });
  },

  unfriend(friendId: number) {
    return apiClient.delete(`/friends/unfriend/${friendId}`);
  },

  searchUsers(query: string) {
    return apiClient.get<UserSearchResult[]>('/friends/search', { params: { query } });
  },
};
