import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box } from '@mui/material';
import ChatHeader from '../../components/chat/ChatHeader';
import MessageList from '../../components/chat/MessageList';
import MessageInput from '../../components/chat/MessageInput';
import RoomMemberList from '../../components/rooms/RoomMemberList';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useAuth } from '../../contexts/AuthContext';
import { useWebSocket } from '../../contexts/WebSocketContext';
import { roomsApi } from '../../api/rooms';
import { filesApi } from '../../api/files';
import { RoomMessage, WebSocketMessage } from '../../types/chat';
import { RoomMember } from '../../types/room';

export default function RoomChatPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { sendMessage: wsSend, addMessageHandler, removeMessageHandler } = useWebSocket();
  const [messages, setMessages] = useState<RoomMessage[]>([]);
  const [members, setMembers] = useState<RoomMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [showMembers, setShowMembers] = useState(false);
  const [roomName, setRoomName] = useState('');

  const fetchMessages = useCallback(async () => {
    if (!roomId) return;
    try {
      const res = await roomsApi.getMessages(roomId);
      setMessages(Array.isArray(res.data) ? res.data : []);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  }, [roomId]);

  const fetchMembers = useCallback(async () => {
    if (!roomId) return;
    try {
      const res = await roomsApi.getMembers(roomId);
      setMembers(Array.isArray(res.data) ? res.data : []);
    } catch {
      // ignore
    }
  }, [roomId]);

  useEffect(() => {
    if (!roomId || !user) return;
    // Join room via WebSocket
    wsSend({ type: 'joinRoom', roomId, sender: user.username });
    fetchMessages();
    fetchMembers();
    roomsApi.markAsRead(roomId).catch(() => {});
  }, [roomId, user, wsSend, fetchMessages, fetchMembers]);

  // WebSocket handler
  useEffect(() => {
    const handler = (msg: WebSocketMessage) => {
      if (msg.roomId === roomId) {
        if (msg.type === 'message') {
          fetchMessages();
        }
        if (msg.type === 'userlist') {
          fetchMembers();
        }
        if (msg.type === 'system' && msg.content) {
          // Add system message
          const sysMsg: RoomMessage = {
            id: Date.now(),
            senderId: 0,
            senderName: 'System',
            content: msg.content,
            messageType: 'TEXT',
            timestamp: new Date().toISOString(),
            readCount: 0,
            isMine: false,
          };
          setMessages((prev) => [...prev, sysMsg]);
        }
      }
    };
    addMessageHandler(`room-${roomId}`, handler);
    return () => removeMessageHandler(`room-${roomId}`);
  }, [roomId, addMessageHandler, removeMessageHandler, fetchMessages, fetchMembers]);

  const handleSend = (content: string) => {
    if (!user || !roomId) return;
    wsSend({
      type: 'message',
      sender: user.username,
      content,
      roomId,
    });
    // Optimistic update
    const newMsg: RoomMessage = {
      id: Date.now(),
      senderId: user.id,
      senderName: user.displayName || user.username,
      content,
      messageType: 'TEXT',
      timestamp: new Date().toISOString(),
      readCount: 0,
      isMine: true,
    };
    setMessages((prev) => [...prev, newMsg]);
  };

  const handleFileUpload = async (file: File) => {
    if (!roomId) return;
    try {
      await filesApi.uploadRoom(file, Number(roomId));
      fetchMessages();
    } catch {
      alert('파일 업로드에 실패했습니다.');
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <ChatHeader
        title={roomName || `채팅방 ${roomId}`}
        subtitle={`${members.length}명`}
        onBack={() => navigate('/chats')}
        onMenu={() => setShowMembers(true)}
      />
      <MessageList messages={messages} currentUserId={user?.id || 0} />
      <MessageInput onSend={handleSend} onFileUpload={handleFileUpload} />
      <RoomMemberList
        open={showMembers}
        onClose={() => setShowMembers(false)}
        members={members}
        roomName={roomName || `채팅방 ${roomId}`}
      />
    </Box>
  );
}
