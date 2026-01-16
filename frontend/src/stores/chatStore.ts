import { create } from 'zustand';
import type { ChatMessage, TypingIndicator } from '../types';

interface ChatState {
  messages: Map<string, ChatMessage[]>; // roomId -> messages
  typingUsers: Map<string, TypingIndicator[]>; // roomId -> typing users
  pendingMessages: ChatMessage[];
  isConnected: boolean;
  connectionError: string | null;
}

interface ChatActions {
  addMessage: (roomId: string, message: ChatMessage) => void;
  setMessages: (roomId: string, messages: ChatMessage[]) => void;
  clearMessages: (roomId: string) => void;
  updateMessage: (roomId: string, messageId: number, updates: Partial<ChatMessage>) => void;
  removeMessage: (roomId: string, messageId: number) => void;

  setTyping: (roomId: string, indicator: TypingIndicator) => void;
  clearTyping: (roomId: string, userId: number) => void;

  addPendingMessage: (message: ChatMessage) => void;
  removePendingMessage: (index: number) => void;
  clearPendingMessages: () => void;

  setConnected: (connected: boolean) => void;
  setConnectionError: (error: string | null) => void;

  addReaction: (roomId: string, messageId: number, emoji: string, userId: number, username: string) => void;
  removeReaction: (roomId: string, messageId: number, reactionId: number) => void;
}

type ChatStore = ChatState & ChatActions;

export const useChatStore = create<ChatStore>((set) => ({
  // State
  messages: new Map(),
  typingUsers: new Map(),
  pendingMessages: [],
  isConnected: false,
  connectionError: null,

  // Actions
  addMessage: (roomId: string, message: ChatMessage) => {
    set((state) => {
      const newMessages = new Map(state.messages);
      const roomMessages = newMessages.get(roomId) || [];

      // 중복 체크
      const exists = message.id && roomMessages.some(m => m.id === message.id);
      if (!exists) {
        newMessages.set(roomId, [...roomMessages, message]);
      }

      return { messages: newMessages };
    });
  },

  setMessages: (roomId: string, messages: ChatMessage[]) => {
    set((state) => {
      const newMessages = new Map(state.messages);
      newMessages.set(roomId, messages);
      return { messages: newMessages };
    });
  },

  clearMessages: (roomId: string) => {
    set((state) => {
      const newMessages = new Map(state.messages);
      newMessages.delete(roomId);
      return { messages: newMessages };
    });
  },

  updateMessage: (roomId: string, messageId: number, updates: Partial<ChatMessage>) => {
    set((state) => {
      const newMessages = new Map(state.messages);
      const roomMessages = newMessages.get(roomId);

      if (roomMessages) {
        const updatedMessages = roomMessages.map(msg =>
          msg.id === messageId ? { ...msg, ...updates } : msg
        );
        newMessages.set(roomId, updatedMessages);
      }

      return { messages: newMessages };
    });
  },

  removeMessage: (roomId: string, messageId: number) => {
    set((state) => {
      const newMessages = new Map(state.messages);
      const roomMessages = newMessages.get(roomId);

      if (roomMessages) {
        newMessages.set(roomId, roomMessages.filter(msg => msg.id !== messageId));
      }

      return { messages: newMessages };
    });
  },

  setTyping: (roomId: string, indicator: TypingIndicator) => {
    set((state) => {
      const newTyping = new Map(state.typingUsers);
      const roomTyping = newTyping.get(roomId) || [];

      // 기존 사용자 업데이트 또는 추가
      const existingIndex = roomTyping.findIndex(t => t.userId === indicator.userId);
      if (existingIndex >= 0) {
        if (indicator.isTyping) {
          roomTyping[existingIndex] = indicator;
        } else {
          roomTyping.splice(existingIndex, 1);
        }
      } else if (indicator.isTyping) {
        roomTyping.push(indicator);
      }

      newTyping.set(roomId, [...roomTyping]);
      return { typingUsers: newTyping };
    });
  },

  clearTyping: (roomId: string, userId: number) => {
    set((state) => {
      const newTyping = new Map(state.typingUsers);
      const roomTyping = newTyping.get(roomId) || [];
      newTyping.set(roomId, roomTyping.filter(t => t.userId !== userId));
      return { typingUsers: newTyping };
    });
  },

  addPendingMessage: (message: ChatMessage) => {
    set((state) => ({
      pendingMessages: [...state.pendingMessages, message],
    }));
  },

  removePendingMessage: (index: number) => {
    set((state) => ({
      pendingMessages: state.pendingMessages.filter((_, i) => i !== index),
    }));
  },

  clearPendingMessages: () => {
    set({ pendingMessages: [] });
  },

  setConnected: (connected: boolean) => {
    set({ isConnected: connected, connectionError: null });
  },

  setConnectionError: (error: string | null) => {
    set({ connectionError: error, isConnected: false });
  },

  addReaction: (roomId: string, messageId: number, emoji: string, userId: number, username: string) => {
    set((state) => {
      const newMessages = new Map(state.messages);
      const roomMessages = newMessages.get(roomId);

      if (roomMessages) {
        const updatedMessages = roomMessages.map(msg => {
          if (msg.id === messageId) {
            const reactions = msg.reactions || [];
            const newReaction = {
              id: Date.now(), // 임시 ID
              emoji,
              userId,
              username,
              createdAt: new Date().toISOString(),
            };
            return { ...msg, reactions: [...reactions, newReaction] };
          }
          return msg;
        });
        newMessages.set(roomId, updatedMessages);
      }

      return { messages: newMessages };
    });
  },

  removeReaction: (roomId: string, messageId: number, reactionId: number) => {
    set((state) => {
      const newMessages = new Map(state.messages);
      const roomMessages = newMessages.get(roomId);

      if (roomMessages) {
        const updatedMessages = roomMessages.map(msg => {
          if (msg.id === messageId && msg.reactions) {
            return {
              ...msg,
              reactions: msg.reactions.filter(r => r.id !== reactionId),
            };
          }
          return msg;
        });
        newMessages.set(roomId, updatedMessages);
      }

      return { messages: newMessages };
    });
  },
}));
