import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { Conversation, DirectMessage, RoomMessage } from '../types/chat';
import { Room } from '../types/room';
import { dmApi } from '../api/dm';
import { roomsApi } from '../api/rooms';
import { useAuth } from './AuthContext';
import { useWebSocket } from './WebSocketContext';
import { WebSocketMessage } from '../types/chat';

interface ChatContextType {
  conversations: Conversation[];
  rooms: Room[];
  totalUnread: number;
  loading: boolean;
  fetchConversations: () => Promise<void>;
  fetchRooms: () => Promise<void>;
  currentMessages: (DirectMessage | RoomMessage)[];
  setCurrentMessages: React.Dispatch<React.SetStateAction<(DirectMessage | RoomMessage)[]>>;
}

const ChatContext = createContext<ChatContextType | null>(null);

export function ChatProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, user } = useAuth();
  const { addMessageHandler, removeMessageHandler } = useWebSocket();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [currentMessages, setCurrentMessages] = useState<(DirectMessage | RoomMessage)[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchConversations = useCallback(async () => {
    try {
      setLoading(true);
      const res = await dmApi.getConversations();
      setConversations(Array.isArray(res.data) ? res.data : []);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchRooms = useCallback(async () => {
    try {
      const res = await roomsApi.getMyRooms();
      setRooms(Array.isArray(res.data) ? res.data : []);
    } catch {
      // ignore
    }
  }, []);

  const totalUnread = conversations.reduce((sum, c) => sum + (c.unreadCount || 0), 0);

  // Listen for WebSocket messages to refresh data
  useEffect(() => {
    if (!isAuthenticated) return;

    const handler = (msg: WebSocketMessage) => {
      if (msg.type === 'message' || msg.type === 'directMessageCreated') {
        fetchConversations();
      }
      if (msg.type === 'roomlist' || msg.type === 'roomDeleted') {
        fetchRooms();
      }
    };

    addMessageHandler('chat-context', handler);
    return () => removeMessageHandler('chat-context');
  }, [isAuthenticated, addMessageHandler, removeMessageHandler, fetchConversations, fetchRooms]);

  useEffect(() => {
    if (isAuthenticated) {
      fetchConversations();
      fetchRooms();
    }
  }, [isAuthenticated, fetchConversations, fetchRooms]);

  return (
    <ChatContext.Provider
      value={{
        conversations,
        rooms,
        totalUnread,
        loading,
        fetchConversations,
        fetchRooms,
        currentMessages,
        setCurrentMessages,
      }}
    >
      {children}
    </ChatContext.Provider>
  );
}

export function useChat() {
  const ctx = useContext(ChatContext);
  if (!ctx) throw new Error('useChat must be used within ChatProvider');
  return ctx;
}
