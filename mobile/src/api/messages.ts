import apiClient from './axios';
import type {ChatMessage} from '../types';

export const messagesApi = {
  getMessages: async (
    roomId: string,
    page: number = 0,
    size: number = 50,
  ): Promise<ChatMessage[]> => {
    const response = await apiClient.get<ChatMessage[]>(
      `/api/v1/rooms/${roomId}/messages`,
      {
        params: {page, size},
      },
    );
    return response.data;
  },

  searchMessages: async (
    roomId: string,
    keyword: string,
  ): Promise<ChatMessage[]> => {
    const response = await apiClient.get<ChatMessage[]>(
      `/api/v1/rooms/${roomId}/messages/search`,
      {
        params: {keyword},
      },
    );
    return response.data;
  },

  deleteMessage: async (roomId: string, messageId: number): Promise<void> => {
    await apiClient.delete(`/api/v1/rooms/${roomId}/messages/${messageId}`);
  },

  addReaction: async (
    roomId: string,
    messageId: number,
    emoji: string,
  ): Promise<void> => {
    await apiClient.post(`/api/v1/rooms/${roomId}/messages/${messageId}/reactions`, {
      emoji,
    });
  },

  removeReaction: async (
    roomId: string,
    messageId: number,
    reactionId: number,
  ): Promise<void> => {
    await apiClient.delete(
      `/api/v1/rooms/${roomId}/messages/${messageId}/reactions/${reactionId}`,
    );
  },
};

export default messagesApi;
