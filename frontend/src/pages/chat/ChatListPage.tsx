import { useState, useEffect } from 'react';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Tab,
  Tabs,
  List,
  Fab,
} from '@mui/material';
import { Add as AddIcon, ChatBubbleOutline } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useChat } from '../../contexts/ChatContext';
import ConversationListItem from '../../components/rooms/ConversationListItem';
import RoomListItem from '../../components/rooms/RoomListItem';
import EmptyState from '../../components/common/EmptyState';
import LoadingSpinner from '../../components/common/LoadingSpinner';

export default function ChatListPage() {
  const navigate = useNavigate();
  const { conversations, rooms, loading, fetchConversations, fetchRooms } = useChat();
  const [tab, setTab] = useState(0);

  useEffect(() => {
    fetchConversations();
    fetchRooms();
  }, [fetchConversations, fetchRooms]);

  return (
    <Box sx={{ height: '100%', position: 'relative' }}>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <Typography variant="h6" sx={{ flex: 1 }}>
            채팅
          </Typography>
        </Toolbar>
        <Tabs
          value={tab}
          onChange={(_, v) => setTab(v)}
          variant="fullWidth"
          sx={{
            '& .MuiTab-root': { fontWeight: 600, minHeight: 40 },
            '& .Mui-selected': { color: '#3C1E1E' },
            '& .MuiTabs-indicator': { bgcolor: '#3C1E1E' },
          }}
        >
          <Tab label={`대화 ${conversations.length}`} />
          <Tab label={`채팅방 ${rooms.length}`} />
        </Tabs>
      </AppBar>

      {loading && conversations.length === 0 && rooms.length === 0 ? (
        <LoadingSpinner />
      ) : tab === 0 ? (
        <List disablePadding>
          {conversations.length === 0 ? (
            <EmptyState
              icon={<ChatBubbleOutline />}
              title="대화가 없습니다"
              description="친구에게 메시지를 보내보세요"
            />
          ) : (
            conversations.map((conv) => (
              <ConversationListItem key={conv.conversationId} conversation={conv} />
            ))
          )}
        </List>
      ) : (
        <List disablePadding>
          {rooms.length === 0 ? (
            <EmptyState
              icon={<ChatBubbleOutline />}
              title="참여한 채팅방이 없습니다"
              description="새 채팅방을 만들어보세요"
            />
          ) : (
            rooms.map((room) => (
              <RoomListItem key={room.roomId || room.id} room={room} />
            ))
          )}
        </List>
      )}

      <Fab
        color="primary"
        size="medium"
        onClick={() => navigate('/rooms/create')}
        sx={{
          position: 'fixed',
          bottom: 72,
          right: 16,
          boxShadow: 2,
        }}
      >
        <AddIcon sx={{ color: '#3C1E1E' }} />
      </Fab>
    </Box>
  );
}
