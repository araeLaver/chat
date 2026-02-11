import apiClient from './client';

export interface SearchResult {
  id: number;
  content: string;
  senderName: string;
  timestamp: string;
  roomId?: string;
  conversationId?: string;
  type: 'DM' | 'ROOM';
}

export const searchApi = {
  searchMessages(keyword: string, type?: 'DM' | 'ROOM') {
    return apiClient.get<SearchResult[]>('/search/messages', {
      params: { keyword, type },
    });
  },
};
