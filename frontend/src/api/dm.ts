import apiClient from './client';
import { DirectMessage, Conversation, SendMessageRequest } from '../types/chat';

export const dmApi = {
  getConversations() {
    return apiClient.get<Conversation[]>('/dm/conversations');
  },

  getMessages(conversationId: string) {
    return apiClient.get<DirectMessage[]>(`/dm/conversation/${conversationId}`);
  },

  sendMessage(data: { receiverId: number; content: string; messageType?: string }) {
    return apiClient.post('/dm/send', data);
  },

  startConversation(friendId: number) {
    return apiClient.post<{ conversationId: string }>('/dm/conversation/start', { friendId });
  },

  markAsRead(conversationId: string) {
    return apiClient.post(`/dm/conversation/${conversationId}/read`);
  },
};
