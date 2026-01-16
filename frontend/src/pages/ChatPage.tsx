import { useState, useEffect } from 'react';
import { Box, Drawer, useMediaQuery, useTheme } from '@mui/material';
import { ChatHeader } from '../components/chat/ChatHeader';
import { ChatSidebar } from '../components/chat/ChatSidebar';
import { ChatWindow } from '../components/chat/ChatWindow';
import { useWebSocket } from '../hooks/useWebSocket';
import { useRoomStore } from '../stores/roomStore';
import { roomsApi, messagesApi } from '../api';

const DRAWER_WIDTH = 320;

export function ChatPage() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);

  const {
    currentRoomId,
    currentConversationId,
    setRooms,
    setConversations,
    setLoading,
  } = useRoomStore();

  // WebSocket 연결
  const { isConnected } = useWebSocket();

  // 초기 데이터 로드
  useEffect(() => {
    const loadInitialData = async () => {
      setLoading(true);
      try {
        const [rooms, conversations] = await Promise.all([
          roomsApi.getMyRooms(),
          messagesApi.getConversations(),
        ]);
        setRooms(rooms);
        setConversations(conversations);
      } catch (error) {
        console.error('Failed to load initial data:', error);
      } finally {
        setLoading(false);
      }
    };

    loadInitialData();
  }, [setRooms, setConversations, setLoading]);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  // 채팅방 선택 시 모바일에서 드로어 닫기
  useEffect(() => {
    if (isMobile && (currentRoomId || currentConversationId)) {
      setMobileOpen(false);
    }
  }, [currentRoomId, currentConversationId, isMobile]);

  const sidebar = (
    <ChatSidebar onClose={() => setMobileOpen(false)} />
  );

  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
      {/* 헤더 */}
      <ChatHeader
        onMenuClick={handleDrawerToggle}
        isConnected={isConnected}
      />

      {/* 사이드바 - 데스크톱 */}
      {!isMobile && (
        <Box
          component="nav"
          sx={{
            width: DRAWER_WIDTH,
            flexShrink: 0,
            mt: '64px',
          }}
        >
          <Box
            sx={{
              width: DRAWER_WIDTH,
              height: 'calc(100vh - 64px)',
              borderRight: 1,
              borderColor: 'divider',
              bgcolor: 'background.paper',
            }}
          >
            {sidebar}
          </Box>
        </Box>
      )}

      {/* 사이드바 - 모바일 */}
      {isMobile && (
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            '& .MuiDrawer-paper': {
              width: DRAWER_WIDTH,
              mt: '56px',
              height: 'calc(100vh - 56px)',
            },
          }}
        >
          {sidebar}
        </Drawer>
      )}

      {/* 메인 채팅 영역 */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          height: '100vh',
          mt: { xs: '56px', sm: '64px' },
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
        }}
      >
        <ChatWindow />
      </Box>
    </Box>
  );
}
