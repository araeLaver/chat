import {useEffect, useRef, useCallback} from 'react';
import {AppState, type AppStateStatus} from 'react-native';
import {useAuthStore} from '../stores/authStore';
import {useChatStore} from '../stores/chatStore';
import type {ChatMessage} from '../types';

const WS_URL = __DEV__
  ? 'ws://10.0.2.2:8080/ws/chat'
  : 'wss://chat-untab-fddd496d.koyeb.app/ws/chat';

const RECONNECT_DELAY = 3000;
const MAX_RECONNECT_ATTEMPTS = 5;
const PING_INTERVAL = 30000;

export function useWebSocket() {
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const pingIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const currentRoomRef = useRef<string | null>(null);

  const {token, user, isAuthenticated} = useAuthStore();
  const {
    addMessage,
    setTyping,
    setConnected,
    setConnectionError,
    updateMessage,
  } = useChatStore();

  const connect = useCallback(() => {
    if (!token || !isAuthenticated) return;
    if (wsRef.current?.readyState === WebSocket.OPEN) return;

    try {
      const wsUrl = `${WS_URL}?token=${encodeURIComponent(token)}`;
      const ws = new WebSocket(wsUrl);

      ws.onopen = () => {
        console.log('WebSocket connected');
        setConnected(true);
        reconnectAttemptsRef.current = 0;

        // Start ping interval
        pingIntervalRef.current = setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({type: 'ping'}));
          }
        }, PING_INTERVAL);
      };

      ws.onmessage = event => {
        try {
          const data = JSON.parse(event.data) as ChatMessage;
          handleMessage(data);
        } catch (error) {
          console.error('Failed to parse message:', error);
        }
      };

      ws.onerror = error => {
        console.error('WebSocket error:', error);
        setConnectionError('연결 오류가 발생했습니다');
      };

      ws.onclose = event => {
        console.log('WebSocket closed:', event.code);
        setConnected(false);
        cleanup();

        // Attempt reconnection
        if (reconnectAttemptsRef.current < MAX_RECONNECT_ATTEMPTS) {
          reconnectTimeoutRef.current = setTimeout(() => {
            reconnectAttemptsRef.current++;
            connect();
          }, RECONNECT_DELAY * Math.pow(2, reconnectAttemptsRef.current));
        } else {
          setConnectionError('연결이 끊어졌습니다. 앱을 다시 시작해주세요.');
        }
      };

      wsRef.current = ws;
    } catch (error) {
      console.error('Failed to create WebSocket:', error);
      setConnectionError('연결에 실패했습니다');
    }
  }, [token, isAuthenticated, setConnected, setConnectionError]);

  const handleMessage = useCallback(
    (data: ChatMessage) => {
      switch (data.type) {
        case 'message':
        case 'image':
        case 'file':
          if (data.roomId) {
            addMessage(data.roomId, data);
          }
          break;

        case 'typing':
        case 'stopTyping':
          if (data.roomId && data.userId && data.sender) {
            setTyping(data.roomId, {
              roomId: data.roomId,
              userId: data.userId,
              username: data.sender,
              isTyping: data.type === 'typing',
            });
          }
          break;

        case 'markAsRead':
          if (data.roomId && data.id) {
            updateMessage(data.roomId, data.id, {isRead: true});
          }
          break;

        case 'pong':
          // Keep-alive response
          break;

        default:
          console.log('Unknown message type:', data.type);
      }
    },
    [addMessage, setTyping, updateMessage],
  );

  const cleanup = useCallback(() => {
    if (pingIntervalRef.current) {
      clearInterval(pingIntervalRef.current);
      pingIntervalRef.current = null;
    }
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
  }, []);

  const disconnect = useCallback(() => {
    cleanup();
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    setConnected(false);
  }, [cleanup, setConnected]);

  const sendMessage = useCallback((message: ChatMessage) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message));
    }
  }, []);

  const joinRoom = useCallback(
    (roomId: string) => {
      currentRoomRef.current = roomId;
      if (wsRef.current?.readyState === WebSocket.OPEN && user) {
        wsRef.current.send(
          JSON.stringify({
            type: 'joinRoom',
            roomId,
            sender: user.username,
            userId: user.id,
          }),
        );
      }
    },
    [user],
  );

  const leaveRoom = useCallback(
    (roomId: string) => {
      currentRoomRef.current = null;
      if (wsRef.current?.readyState === WebSocket.OPEN && user) {
        wsRef.current.send(
          JSON.stringify({
            type: 'leaveRoom',
            roomId,
            sender: user.username,
            userId: user.id,
          }),
        );
      }
    },
    [user],
  );

  const sendTyping = useCallback(
    (roomId: string, isTyping: boolean) => {
      if (wsRef.current?.readyState === WebSocket.OPEN && user) {
        wsRef.current.send(
          JSON.stringify({
            type: isTyping ? 'typing' : 'stopTyping',
            roomId,
            sender: user.username,
            userId: user.id,
          }),
        );
      }
    },
    [user],
  );

  // Handle app state changes
  useEffect(() => {
    const handleAppStateChange = (nextAppState: AppStateStatus) => {
      if (nextAppState === 'active' && isAuthenticated) {
        connect();
      } else if (nextAppState === 'background') {
        // Keep connection for background messages
      }
    };

    const subscription = AppState.addEventListener(
      'change',
      handleAppStateChange,
    );

    return () => {
      subscription.remove();
    };
  }, [connect, isAuthenticated]);

  // Connect when authenticated
  useEffect(() => {
    if (isAuthenticated) {
      connect();
    } else {
      disconnect();
    }

    return () => {
      disconnect();
    };
  }, [isAuthenticated, connect, disconnect]);

  return {
    isConnected: useChatStore.getState().isConnected,
    connectionError: useChatStore.getState().connectionError,
    sendMessage,
    joinRoom,
    leaveRoom,
    sendTyping,
    reconnect: connect,
  };
}
