import apiClient from './axios';
import type { MessageEntity, DirectMessage, Conversation } from '../types';

export const messagesApi = {
  // =========== 채팅방 메시지 ===========

  // 채팅방 메시지 조회
  getRoomMessages: async (roomId: string, page = 0, size = 50): Promise<{
    content: MessageEntity[];
    totalPages: number;
    totalElements: number;
  }> => {
    const response = await apiClient.get(`/api/rooms/${roomId}/messages`, {
      params: { page, size },
    });
    return response.data;
  },

  // 채팅방 메시지 전체 읽음 처리
  markRoomAsRead: async (roomId: string): Promise<void> => {
    await apiClient.post(`/api/rooms/${roomId}/read`);
  },

  // =========== DM (1:1 메시지) ===========

  // 대화 목록 조회
  getConversations: async (): Promise<Conversation[]> => {
    const response = await apiClient.get<Conversation[]>('/api/dm/conversations');
    return response.data;
  },

  // 대화 메시지 조회
  getConversationMessages: async (conversationId: number): Promise<DirectMessage[]> => {
    const response = await apiClient.get<DirectMessage[]>(`/api/dm/conversation/${conversationId}`);
    return response.data;
  },

  // DM 전송
  sendDirectMessage: async (receiverId: number, content: string, options?: {
    ttlSeconds?: number;
    sourceLanguage?: string;
  }): Promise<DirectMessage> => {
    const response = await apiClient.post<DirectMessage>('/api/dm/send', {
      receiverId,
      content,
      ...options,
    });
    return response.data;
  },

  // DM 읽음 처리
  markConversationAsRead: async (conversationId: number): Promise<void> => {
    await apiClient.post(`/api/dm/conversation/${conversationId}/read`);
  },

  // 대화 시작
  startConversation: async (userId: number): Promise<Conversation> => {
    const response = await apiClient.post<Conversation>('/api/dm/conversation/start', { targetUserId: userId });
    return response.data;
  },

  // =========== 리액션 ===========

  // 리액션 추가
  addReaction: async (messageId: number, emoji: string, messageType: 'ROOM' | 'DM' = 'ROOM'): Promise<void> => {
    await apiClient.post('/api/reactions', { messageId, emoji, messageType });
  },

  // 리액션 제거
  removeReaction: async (messageId: number, emoji: string, messageType: 'ROOM' | 'DM' = 'ROOM'): Promise<void> => {
    await apiClient.delete('/api/reactions', { data: { messageId, emoji, messageType } });
  },

  // 리액션 토글
  toggleReaction: async (messageId: number, emoji: string, messageType: 'ROOM' | 'DM' = 'ROOM'): Promise<void> => {
    await apiClient.post('/api/reactions/toggle', { messageId, emoji, messageType });
  },

  // =========== 북마크 ===========

  // 북마크 추가
  addBookmark: async (messageId: number): Promise<void> => {
    await apiClient.post(`/api/messages/${messageId}/bookmark`);
  },

  // 북마크 제거
  removeBookmark: async (messageId: number): Promise<void> => {
    await apiClient.delete(`/api/messages/${messageId}/bookmark`);
  },

  // 북마크 목록
  getBookmarks: async (): Promise<MessageEntity[]> => {
    const response = await apiClient.get<MessageEntity[]>('/api/messages/bookmarks');
    return response.data;
  },

  // =========== 멘션 ===========

  // 멘션 목록
  getMentions: async (unreadOnly = false): Promise<MessageEntity[]> => {
    const response = await apiClient.get<MessageEntity[]>('/api/messages/mentions', {
      params: { unreadOnly },
    });
    return response.data;
  },

  // 멘션 읽음 처리
  markMentionAsRead: async (mentionId: number): Promise<void> => {
    await apiClient.post(`/api/messages/mentions/${mentionId}/read`);
  },

  // =========== 번역 ===========

  // 메시지 번역
  translateMessage: async (content: string, targetLanguage: string): Promise<{
    translatedText: string;
    sourceLanguage: string;
  }> => {
    const response = await apiClient.post('/api/translate', {
      text: content,
      targetLanguage,
    });
    return response.data;
  },
};
