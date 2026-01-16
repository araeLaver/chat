import { Box, Typography, Paper } from '@mui/material';
import { Chat as ChatIcon } from '@mui/icons-material';
import { useRoomStore } from '../../stores/roomStore';
import { MessageList } from './MessageList';
import { MessageInput } from './MessageInput';
import { ChatRoomHeader } from './ChatRoomHeader';
import { useChat } from '../../hooks/useChat';

export function ChatWindow() {
  const {
    currentRoomId,
    currentRoom,
    currentConversationId,
    currentConversation,
  } = useRoomStore();

  // 채팅방 또는 대화 ID
  const roomId = currentRoomId || currentConversationId;
  const title = currentRoom?.name || currentConversation?.otherUser?.username;

  const {
    messages,
    typingUsers,
    isConnected,
    sendMessage,
    sendFile,
    handleTyping,
  } = useChat(roomId || undefined);

  // 선택된 방이 없는 경우
  if (!roomId) {
    return (
      <Box
        sx={{
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: 'background.default',
        }}
      >
        <ChatIcon sx={{ fontSize: 80, color: 'text.disabled', mb: 2 }} />
        <Typography variant="h6" color="text.secondary">
          채팅을 시작하려면 채팅방을 선택하세요
        </Typography>
        <Typography variant="body2" color="text.disabled" sx={{ mt: 1 }}>
          왼쪽 사이드바에서 채팅방을 선택하거나 새 채팅방을 만들 수 있습니다
        </Typography>
      </Box>
    );
  }

  return (
    <Box
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: 'background.default',
      }}
    >
      {/* 채팅방 헤더 */}
      <ChatRoomHeader
        title={title || '채팅'}
        subtitle={currentRoom ? `${currentRoom.users?.length || 0}명 참여 중` : undefined}
        isOnline={currentConversation?.otherUser?.isOnline}
        typingUsers={typingUsers}
      />

      {/* 메시지 목록 */}
      <Box sx={{ flex: 1, overflow: 'hidden' }}>
        <MessageList
          messages={messages}
          roomId={roomId}
        />
      </Box>

      {/* 메시지 입력 */}
      <Paper
        elevation={2}
        sx={{
          borderRadius: 0,
          borderTop: 1,
          borderColor: 'divider',
        }}
      >
        <MessageInput
          onSendMessage={sendMessage}
          onSendFile={sendFile}
          onTyping={handleTyping}
          disabled={!isConnected}
        />
      </Paper>
    </Box>
  );
}
