import { useCallback, useEffect, useRef } from 'react';
import { useChatStore } from '../stores/chatStore';
import { useRoomStore } from '../stores/roomStore';
import { useWebSocket } from './useWebSocket';
import { messagesApi, filesApi } from '../api';

export function useChat(roomId?: string) {
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isTypingRef = useRef(false);

  const {
    messages,
    typingUsers,
    isConnected,
  } = useChatStore();

  const { currentRoom, resetUnread } = useRoomStore();

  const {
    sendMessage: wsSendMessage,
    sendFileMessage: wsSendFileMessage,
    sendTyping,
    joinRoom,
    leaveRoom,
    markAsRead,
    requestHistory,
  } = useWebSocket();

  // 현재 방의 메시지 목록
  const roomMessages = roomId ? messages.get(roomId) || [] : [];

  // 현재 방의 타이핑 사용자
  const roomTypingUsers = roomId ? typingUsers.get(roomId) || [] : [];

  // 방 입장 시 히스토리 로드
  useEffect(() => {
    if (roomId && isConnected) {
      joinRoom(roomId);
      requestHistory(roomId);
      markAsRead(roomId);
      resetUnread(roomId);
    }

    return () => {
      if (roomId && isConnected) {
        leaveRoom(roomId);
      }
    };
  }, [roomId, isConnected, joinRoom, leaveRoom, requestHistory, markAsRead, resetUnread]);

  // 메시지 전송
  const sendMessage = useCallback((content: string, options?: {
    ttlSeconds?: number;
    replyToId?: number;
    mentions?: string[];
  }) => {
    if (!roomId || !content.trim()) return;

    wsSendMessage(content, roomId, options);

    // 타이핑 상태 해제
    if (isTypingRef.current) {
      sendTyping(roomId, false);
      isTypingRef.current = false;
    }
  }, [roomId, wsSendMessage, sendTyping]);

  // 파일 업로드 및 전송
  const sendFile = useCallback(async (file: File) => {
    if (!roomId) return;

    try {
      const uploadedFile = await filesApi.upload(file, roomId);
      wsSendFileMessage(roomId, {
        fileName: uploadedFile.originalName,
        fileUrl: uploadedFile.url,
        fileSize: uploadedFile.size,
        mimeType: uploadedFile.contentType,
      });
    } catch (error) {
      console.error('Failed to upload file:', error);
      throw error;
    }
  }, [roomId, wsSendFileMessage]);

  // 타이핑 표시 (디바운스)
  const handleTyping = useCallback(() => {
    if (!roomId) return;

    // 타이핑 시작
    if (!isTypingRef.current) {
      isTypingRef.current = true;
      sendTyping(roomId, true);
    }

    // 기존 타임아웃 취소
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    // 2초 후 타이핑 중지
    typingTimeoutRef.current = setTimeout(() => {
      if (isTypingRef.current) {
        isTypingRef.current = false;
        sendTyping(roomId, false);
      }
    }, 2000);
  }, [roomId, sendTyping]);

  // 리액션 토글
  const toggleReaction = useCallback(async (messageId: number, emoji: string) => {
    try {
      await messagesApi.toggleReaction(messageId, emoji, 'ROOM');
    } catch (error) {
      console.error('Failed to toggle reaction:', error);
      throw error;
    }
  }, []);

  // 메시지 히스토리 로드 (페이징)
  const loadMoreMessages = useCallback(async (page: number) => {
    if (!roomId) return null;

    try {
      const response = await messagesApi.getRoomMessages(roomId, page);
      return response;
    } catch (error) {
      console.error('Failed to load messages:', error);
      throw error;
    }
  }, [roomId]);

  // 정리
  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
    };
  }, []);

  return {
    messages: roomMessages,
    typingUsers: roomTypingUsers,
    isConnected,
    currentRoom,
    sendMessage,
    sendFile,
    handleTyping,
    toggleReaction,
    loadMoreMessages,
    markAsRead: () => roomId && markAsRead(roomId),
  };
}
