import { create } from 'zustand';
import type { ChatRoom, GroupRoom, Conversation } from '../types';

interface RoomState {
  // 일반 채팅방
  rooms: ChatRoom[];
  currentRoomId: string | null;
  currentRoom: ChatRoom | null;

  // 그룹 채팅방
  groupRooms: GroupRoom[];
  currentGroupId: number | null;
  currentGroup: GroupRoom | null;

  // DM 대화
  conversations: Conversation[];
  currentConversationId: string | null;
  currentConversation: Conversation | null;

  // UI 상태
  isLoading: boolean;
  error: string | null;
  sidebarTab: 'rooms' | 'dm' | 'friends';
}

interface RoomActions {
  // 일반 채팅방
  setRooms: (rooms: ChatRoom[]) => void;
  addRoom: (room: ChatRoom) => void;
  updateRoom: (roomId: string, updates: Partial<ChatRoom>) => void;
  removeRoom: (roomId: string) => void;
  setCurrentRoom: (roomId: string | null) => void;

  // 그룹 채팅방
  setGroupRooms: (rooms: GroupRoom[]) => void;
  addGroupRoom: (room: GroupRoom) => void;
  updateGroupRoom: (roomId: number, updates: Partial<GroupRoom>) => void;
  removeGroupRoom: (roomId: number) => void;
  setCurrentGroup: (roomId: number | null) => void;

  // DM 대화
  setConversations: (conversations: Conversation[]) => void;
  addConversation: (conversation: Conversation) => void;
  updateConversation: (conversationId: string, updates: Partial<Conversation>) => void;
  removeConversation: (conversationId: string) => void;
  setCurrentConversation: (conversationId: string | null) => void;

  // UI
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  setSidebarTab: (tab: 'rooms' | 'dm' | 'friends') => void;

  // 안읽은 메시지 수 업데이트
  incrementUnread: (roomId: string) => void;
  resetUnread: (roomId: string) => void;
}

type RoomStore = RoomState & RoomActions;

export const useRoomStore = create<RoomStore>((set, get) => ({
  // State
  rooms: [],
  currentRoomId: null,
  currentRoom: null,

  groupRooms: [],
  currentGroupId: null,
  currentGroup: null,

  conversations: [],
  currentConversationId: null,
  currentConversation: null,

  isLoading: false,
  error: null,
  sidebarTab: 'rooms',

  // 일반 채팅방 Actions
  setRooms: (rooms: ChatRoom[]) => {
    set({ rooms });
  },

  addRoom: (room: ChatRoom) => {
    set((state) => ({
      rooms: [...state.rooms, room],
    }));
  },

  updateRoom: (roomId: string, updates: Partial<ChatRoom>) => {
    set((state) => ({
      rooms: state.rooms.map(r =>
        r.id === roomId ? { ...r, ...updates } : r
      ),
      currentRoom: state.currentRoom?.id === roomId
        ? { ...state.currentRoom, ...updates }
        : state.currentRoom,
    }));
  },

  removeRoom: (roomId: string) => {
    set((state) => ({
      rooms: state.rooms.filter(r => r.id !== roomId),
      currentRoomId: state.currentRoomId === roomId ? null : state.currentRoomId,
      currentRoom: state.currentRoom?.id === roomId ? null : state.currentRoom,
    }));
  },

  setCurrentRoom: (roomId: string | null) => {
    const room = roomId ? get().rooms.find(r => r.id === roomId) || null : null;
    set({
      currentRoomId: roomId,
      currentRoom: room,
      currentGroupId: null,
      currentGroup: null,
      currentConversationId: null,
      currentConversation: null,
    });
  },

  // 그룹 채팅방 Actions
  setGroupRooms: (rooms: GroupRoom[]) => {
    set({ groupRooms: rooms });
  },

  addGroupRoom: (room: GroupRoom) => {
    set((state) => ({
      groupRooms: [...state.groupRooms, room],
    }));
  },

  updateGroupRoom: (roomId: number, updates: Partial<GroupRoom>) => {
    set((state) => ({
      groupRooms: state.groupRooms.map(r =>
        r.id === roomId ? { ...r, ...updates } : r
      ),
      currentGroup: state.currentGroup?.id === roomId
        ? { ...state.currentGroup, ...updates }
        : state.currentGroup,
    }));
  },

  removeGroupRoom: (roomId: number) => {
    set((state) => ({
      groupRooms: state.groupRooms.filter(r => r.id !== roomId),
      currentGroupId: state.currentGroupId === roomId ? null : state.currentGroupId,
      currentGroup: state.currentGroup?.id === roomId ? null : state.currentGroup,
    }));
  },

  setCurrentGroup: (roomId: number | null) => {
    const room = roomId ? get().groupRooms.find(r => r.id === roomId) || null : null;
    set({
      currentGroupId: roomId,
      currentGroup: room,
      currentRoomId: null,
      currentRoom: null,
      currentConversationId: null,
      currentConversation: null,
    });
  },

  // DM 대화 Actions
  setConversations: (conversations: Conversation[]) => {
    set({ conversations });
  },

  addConversation: (conversation: Conversation) => {
    set((state) => ({
      conversations: [...state.conversations, conversation],
    }));
  },

  updateConversation: (conversationId: string, updates: Partial<Conversation>) => {
    set((state) => ({
      conversations: state.conversations.map(c =>
        c.conversationId === conversationId ? { ...c, ...updates } : c
      ),
      currentConversation: state.currentConversation?.conversationId === conversationId
        ? { ...state.currentConversation, ...updates }
        : state.currentConversation,
    }));
  },

  removeConversation: (conversationId: string) => {
    set((state) => ({
      conversations: state.conversations.filter(c => c.conversationId !== conversationId),
      currentConversationId: state.currentConversationId === conversationId ? null : state.currentConversationId,
      currentConversation: state.currentConversation?.conversationId === conversationId ? null : state.currentConversation,
    }));
  },

  setCurrentConversation: (conversationId: string | null) => {
    const conversation = conversationId
      ? get().conversations.find(c => c.conversationId === conversationId) || null
      : null;
    set({
      currentConversationId: conversationId,
      currentConversation: conversation,
      currentRoomId: null,
      currentRoom: null,
      currentGroupId: null,
      currentGroup: null,
    });
  },

  // UI Actions
  setLoading: (loading: boolean) => {
    set({ isLoading: loading });
  },

  setError: (error: string | null) => {
    set({ error });
  },

  setSidebarTab: (tab: 'rooms' | 'dm' | 'friends') => {
    set({ sidebarTab: tab });
  },

  // 안읽은 메시지 수
  incrementUnread: (roomId: string) => {
    set((state) => ({
      rooms: state.rooms.map(r =>
        r.id === roomId ? { ...r, unreadCount: (r.unreadCount || 0) + 1 } : r
      ),
    }));
  },

  resetUnread: (roomId: string) => {
    set((state) => ({
      rooms: state.rooms.map(r =>
        r.id === roomId ? { ...r, unreadCount: 0 } : r
      ),
    }));
  },
}));
