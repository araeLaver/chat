import { useEffect, useRef, useCallback, useState } from 'react';
import { useAuthStore } from '../stores/authStore';
import { useChatStore } from '../stores/chatStore';
import type { ChatMessage } from '../types';

const WS_URL = import.meta.env.VITE_WS_URL || `ws://${window.location.host}/ws/chat`;
const RECONNECT_INTERVAL = 3000;
const MAX_RECONNECT_ATTEMPTS = 10;
const PING_INTERVAL = 30000;

interface UseWebSocketOptions {
  onMessage?: (message: ChatMessage) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: Event) => void;
  autoConnect?: boolean;
}

export function useWebSocket(options: UseWebSocketOptions = {}) {
  const {
    onMessage,
    onConnect,
    onDisconnect,
    onError,
    autoConnect = true,
  } = options;

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectAttempts = useRef(0);
  const reconnectTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pingInterval = useRef<ReturnType<typeof setInterval> | null>(null);
  const messageQueue = useRef<ChatMessage[]>([]);

  const [isConnected, setIsConnected] = useState(false);
  const [lastMessage, setLastMessage] = useState<ChatMessage | null>(null);

  const { token, isAuthenticated, user } = useAuthStore();
  const { setConnected, setConnectionError, addMessage, setTyping } = useChatStore();

  // 메시지 전송
  const send = useCallback((message: ChatMessage) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message));
    } else {
      // 연결되지 않은 경우 큐에 저장
      messageQueue.current.push(message);
    }
  }, []);

  // 큐에 있는 메시지 전송
  const flushMessageQueue = useCallback(() => {
    while (messageQueue.current.length > 0 && wsRef.current?.readyState === WebSocket.OPEN) {
      const message = messageQueue.current.shift();
      if (message) {
        wsRef.current.send(JSON.stringify(message));
      }
    }
  }, []);

  // Ping 전송 (연결 유지)
  const startPing = useCallback(() => {
    pingInterval.current = setInterval(() => {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        send({
          type: 'ping',
          sender: user?.username || 'anonymous',
          content: '',
          timestamp: new Date().toISOString(),
        });
      }
    }, PING_INTERVAL);
  }, [send, user?.username]);

  const stopPing = useCallback(() => {
    if (pingInterval.current) {
      clearInterval(pingInterval.current);
      pingInterval.current = null;
    }
  }, []);

  // WebSocket 연결
  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      return;
    }

    try {
      const url = token ? `${WS_URL}?token=${token}` : WS_URL;
      const ws = new WebSocket(url);

      ws.onopen = () => {
        console.log('[WebSocket] Connected');
        setIsConnected(true);
        setConnected(true);
        reconnectAttempts.current = 0;
        flushMessageQueue();
        startPing();
        onConnect?.();
      };

      ws.onmessage = (event) => {
        try {
          const message: ChatMessage = JSON.parse(event.data);

          // pong 메시지는 무시
          if (message.type === 'pong') {
            return;
          }

          setLastMessage(message);

          // 메시지 타입에 따른 처리
          switch (message.type) {
            case 'message':
            case 'file':
            case 'image':
              if (message.roomId) {
                addMessage(message.roomId, message);
              }
              break;

            case 'typing':
            case 'stopTyping':
              if (message.roomId && message.userId) {
                setTyping(message.roomId, {
                  roomId: message.roomId,
                  userId: message.userId,
                  username: message.sender,
                  isTyping: message.type === 'typing',
                });
              }
              break;

            case 'system':
              // 시스템 메시지 (입장/퇴장 등)
              if (message.roomId) {
                addMessage(message.roomId, message);
              }
              break;
          }

          onMessage?.(message);
        } catch (error) {
          console.error('[WebSocket] Failed to parse message:', error);
        }
      };

      ws.onclose = (event) => {
        console.log('[WebSocket] Disconnected:', event.code, event.reason);
        setIsConnected(false);
        setConnected(false);
        stopPing();
        onDisconnect?.();

        // 자동 재연결
        if (isAuthenticated && reconnectAttempts.current < MAX_RECONNECT_ATTEMPTS) {
          const delay = Math.min(
            RECONNECT_INTERVAL * Math.pow(2, reconnectAttempts.current),
            30000
          );
          console.log(`[WebSocket] Reconnecting in ${delay}ms...`);

          reconnectTimeout.current = setTimeout(() => {
            reconnectAttempts.current++;
            connect();
          }, delay);
        } else if (reconnectAttempts.current >= MAX_RECONNECT_ATTEMPTS) {
          setConnectionError('연결 재시도 횟수를 초과했습니다.');
        }
      };

      ws.onerror = (error) => {
        console.error('[WebSocket] Error:', error);
        setConnectionError('WebSocket 연결 오류');
        onError?.(error);
      };

      wsRef.current = ws;
    } catch (error) {
      console.error('[WebSocket] Failed to connect:', error);
      setConnectionError('WebSocket 연결 실패');
    }
  }, [
    token,
    isAuthenticated,
    setConnected,
    setConnectionError,
    addMessage,
    setTyping,
    flushMessageQueue,
    startPing,
    stopPing,
    onConnect,
    onDisconnect,
    onMessage,
    onError,
  ]);

  // 연결 해제
  const disconnect = useCallback(() => {
    if (reconnectTimeout.current) {
      clearTimeout(reconnectTimeout.current);
    }
    stopPing();

    if (wsRef.current) {
      wsRef.current.close(1000, 'User disconnected');
      wsRef.current = null;
    }

    setIsConnected(false);
    setConnected(false);
  }, [setConnected, stopPing]);

  // 채팅방 입장
  const joinRoom = useCallback((roomId: string) => {
    send({
      type: 'joinRoom',
      sender: user?.username || 'anonymous',
      content: roomId,
      timestamp: new Date().toISOString(),
      roomId,
      userId: user?.id,
    });
  }, [send, user]);

  // 채팅방 퇴장
  const leaveRoom = useCallback((roomId: string) => {
    send({
      type: 'leaveRoom',
      sender: user?.username || 'anonymous',
      content: roomId,
      timestamp: new Date().toISOString(),
      roomId,
      userId: user?.id,
    });
  }, [send, user]);

  // 메시지 전송
  const sendMessage = useCallback((content: string, roomId: string, options?: {
    ttlSeconds?: number;
    replyToId?: number;
    mentions?: string[];
  }) => {
    send({
      type: 'message',
      sender: user?.username || 'anonymous',
      content,
      timestamp: new Date().toISOString(),
      roomId,
      userId: user?.id,
      ...options,
    });
  }, [send, user]);

  // 파일 메시지 전송
  const sendFileMessage = useCallback((roomId: string, fileInfo: {
    fileName: string;
    fileUrl: string;
    fileSize: number;
    mimeType: string;
  }) => {
    const type = fileInfo.mimeType.startsWith('image/') ? 'image' : 'file';
    send({
      type,
      sender: user?.username || 'anonymous',
      content: fileInfo.fileName,
      timestamp: new Date().toISOString(),
      roomId,
      userId: user?.id,
      ...fileInfo,
    });
  }, [send, user]);

  // 타이핑 표시
  const sendTyping = useCallback((roomId: string, isTyping: boolean) => {
    send({
      type: isTyping ? 'typing' : 'stopTyping',
      sender: user?.username || 'anonymous',
      content: '',
      timestamp: new Date().toISOString(),
      roomId,
      userId: user?.id,
    });
  }, [send, user]);

  // 히스토리 요청
  const requestHistory = useCallback((roomId: string) => {
    send({
      type: 'getHistory',
      sender: user?.username || 'anonymous',
      content: '',
      timestamp: new Date().toISOString(),
      roomId,
      userId: user?.id,
    });
  }, [send, user]);

  // 읽음 표시
  const markAsRead = useCallback((roomId: string) => {
    send({
      type: 'markAsRead',
      sender: user?.username || 'anonymous',
      content: '',
      timestamp: new Date().toISOString(),
      roomId,
      userId: user?.id,
    });
  }, [send, user]);

  // 자동 연결/해제
  useEffect(() => {
    if (autoConnect && isAuthenticated) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [autoConnect, isAuthenticated, connect, disconnect]);

  return {
    isConnected,
    lastMessage,
    connect,
    disconnect,
    send,
    joinRoom,
    leaveRoom,
    sendMessage,
    sendFileMessage,
    sendTyping,
    requestHistory,
    markAsRead,
  };
}
