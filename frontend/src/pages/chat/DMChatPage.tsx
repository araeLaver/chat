import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box } from '@mui/material';
import ChatHeader from '../../components/chat/ChatHeader';
import MessageList from '../../components/chat/MessageList';
import MessageInput from '../../components/chat/MessageInput';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useAuth } from '../../contexts/AuthContext';
import { useWebSocket } from '../../contexts/WebSocketContext';
import { dmApi } from '../../api/dm';
import { filesApi } from '../../api/files';
import { DirectMessage, WebSocketMessage } from '../../types/chat';

export default function DMChatPage() {
  const { conversationId } = useParams<{ conversationId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { sendMessage: wsSend, addMessageHandler, removeMessageHandler } = useWebSocket();
  const [messages, setMessages] = useState<DirectMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [otherName, setOtherName] = useState('');

  const fetchMessages = useCallback(async () => {
    if (!conversationId) return;
    try {
      const res = await dmApi.getMessages(conversationId);
      const msgs = Array.isArray(res.data) ? res.data : [];
      setMessages(msgs);
      // Extract other user's name from messages
      if (msgs.length > 0 && user) {
        const other = msgs.find((m) => m.senderId !== user.id);
        if (other) {
          setOtherName((other as any).senderName || (other as any).senderDisplayName || '상대방');
        }
      }
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  }, [conversationId, user]);

  useEffect(() => {
    fetchMessages();
    // Mark as read
    if (conversationId) {
      dmApi.markAsRead(conversationId).catch(() => {});
    }
  }, [fetchMessages, conversationId]);

  // WebSocket handler for real-time messages
  useEffect(() => {
    const handler = (msg: WebSocketMessage) => {
      if (
        msg.type === 'message' &&
        msg.roomId === conversationId
      ) {
        // Refresh messages when a new message comes in this conversation
        fetchMessages();
      }
    };
    addMessageHandler(`dm-${conversationId}`, handler);
    return () => removeMessageHandler(`dm-${conversationId}`);
  }, [conversationId, addMessageHandler, removeMessageHandler, fetchMessages]);

  const handleSend = (content: string) => {
    if (!user || !conversationId) return;
    // Send via WebSocket
    wsSend({
      type: 'message',
      sender: user.username,
      content,
      roomId: conversationId,
    });
    // Optimistic update
    const newMsg: DirectMessage = {
      messageId: Date.now(),
      conversationId: conversationId,
      senderId: user.id,
      receiverId: 0,
      content,
      timestamp: new Date().toISOString(),
      isRead: false,
      messageType: 'TEXT',
      isMine: true,
    };
    setMessages((prev) => [...prev, newMsg]);
  };

  const handleFileUpload = async (file: File) => {
    if (!conversationId) return;
    try {
      await filesApi.uploadDM(file, conversationId);
      fetchMessages();
    } catch {
      alert('파일 업로드에 실패했습니다.');
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <ChatHeader
        title={otherName || conversationId || '채팅'}
        onBack={() => navigate('/chats')}
      />
      <MessageList messages={messages} currentUserId={user?.id || 0} />
      <MessageInput onSend={handleSend} onFileUpload={handleFileUpload} />
    </Box>
  );
}
