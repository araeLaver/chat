import { useState } from 'react';
import {
  Box,
  Tabs,
  Tab,
  List,
  ListItemButton,
  ListItemAvatar,
  ListItemText,
  Typography,
  Badge,
  Fab,
  TextField,
  InputAdornment,
  IconButton,
} from '@mui/material';
import {
  Chat as ChatIcon,
  Person,
  Group,
  Add,
  Search,
  Close,
} from '@mui/icons-material';
import { Avatar } from '../common/Avatar';
import { useRoomStore } from '../../stores/roomStore';
import { CreateRoomDialog } from '../rooms/CreateRoomDialog';
import { formatDistanceToNow } from 'date-fns';
import { ko } from 'date-fns/locale';

interface ChatSidebarProps {
  onClose?: () => void;
}

export function ChatSidebar({ onClose }: ChatSidebarProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);

  const {
    rooms,
    conversations,
    currentRoomId,
    currentConversationId,
    sidebarTab,
    setSidebarTab,
    setCurrentRoom,
    setCurrentConversation,
  } = useRoomStore();

  const handleTabChange = (_event: React.SyntheticEvent, newValue: 'rooms' | 'dm' | 'friends') => {
    setSidebarTab(newValue);
  };

  const handleRoomClick = (roomId: string) => {
    setCurrentRoom(roomId);
    onClose?.();
  };

  const handleConversationClick = (conversationId: string) => {
    setCurrentConversation(conversationId);
    onClose?.();
  };

  // 검색 필터링
  const filteredRooms = rooms.filter((room) =>
    room.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredConversations = conversations.filter((conv) =>
    conv.otherUser?.username.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const formatTime = (timestamp?: string) => {
    if (!timestamp) return '';
    try {
      return formatDistanceToNow(new Date(timestamp), { addSuffix: true, locale: ko });
    } catch {
      return '';
    }
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 검색 */}
      <Box sx={{ p: 1 }}>
        <TextField
          fullWidth
          size="small"
          placeholder="검색..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <Search fontSize="small" />
              </InputAdornment>
            ),
            endAdornment: searchQuery && (
              <InputAdornment position="end">
                <IconButton size="small" onClick={() => setSearchQuery('')}>
                  <Close fontSize="small" />
                </IconButton>
              </InputAdornment>
            ),
          }}
        />
      </Box>

      {/* 탭 */}
      <Tabs
        value={sidebarTab}
        onChange={handleTabChange}
        variant="fullWidth"
        sx={{ borderBottom: 1, borderColor: 'divider' }}
      >
        <Tab
          value="rooms"
          icon={<ChatIcon fontSize="small" />}
          label="채팅방"
          sx={{ minHeight: 48 }}
        />
        <Tab
          value="dm"
          icon={<Person fontSize="small" />}
          label="DM"
          sx={{ minHeight: 48 }}
        />
        <Tab
          value="friends"
          icon={<Group fontSize="small" />}
          label="친구"
          sx={{ minHeight: 48 }}
        />
      </Tabs>

      {/* 채팅방 목록 */}
      {sidebarTab === 'rooms' && (
        <List sx={{ flex: 1, overflow: 'auto' }}>
          {filteredRooms.length === 0 ? (
            <Box sx={{ p: 3, textAlign: 'center' }}>
              <Typography variant="body2" color="text.secondary">
                {searchQuery ? '검색 결과가 없습니다.' : '참여 중인 채팅방이 없습니다.'}
              </Typography>
            </Box>
          ) : (
            filteredRooms.map((room) => (
              <ListItemButton
                key={room.id}
                selected={currentRoomId === room.id}
                onClick={() => handleRoomClick(room.id)}
              >
                <ListItemAvatar>
                  <Badge
                    badgeContent={room.unreadCount}
                    color="error"
                    max={99}
                  >
                    <Avatar name={room.name} />
                  </Badge>
                </ListItemAvatar>
                <ListItemText
                  primary={room.name}
                  secondary={room.lastMessage || '새 채팅방'}
                  primaryTypographyProps={{
                    fontWeight: room.unreadCount ? 'bold' : 'normal',
                    noWrap: true,
                  }}
                  secondaryTypographyProps={{ noWrap: true }}
                />
                {room.lastMessageTime && (
                  <Typography variant="caption" color="text.secondary">
                    {formatTime(room.lastMessageTime)}
                  </Typography>
                )}
              </ListItemButton>
            ))
          )}
        </List>
      )}

      {/* DM 목록 */}
      {sidebarTab === 'dm' && (
        <List sx={{ flex: 1, overflow: 'auto' }}>
          {filteredConversations.length === 0 ? (
            <Box sx={{ p: 3, textAlign: 'center' }}>
              <Typography variant="body2" color="text.secondary">
                {searchQuery ? '검색 결과가 없습니다.' : '대화가 없습니다.'}
              </Typography>
            </Box>
          ) : (
            filteredConversations.map((conv) => {
              const unreadCount = conv.unreadCount1 || conv.unreadCount2 || 0;
              return (
                <ListItemButton
                  key={conv.conversationId}
                  selected={currentConversationId === conv.conversationId}
                  onClick={() => handleConversationClick(conv.conversationId)}
                >
                  <ListItemAvatar>
                    <Badge badgeContent={unreadCount} color="error" max={99}>
                      <Avatar
                        name={conv.otherUser?.username}
                        src={conv.otherUser?.profileImage}
                        isOnline={conv.otherUser?.isOnline}
                        showStatus
                      />
                    </Badge>
                  </ListItemAvatar>
                  <ListItemText
                    primary={conv.otherUser?.username || '알 수 없음'}
                    secondary={conv.lastMessage || '대화 시작하기'}
                    primaryTypographyProps={{
                      fontWeight: unreadCount > 0 ? 'bold' : 'normal',
                      noWrap: true,
                    }}
                    secondaryTypographyProps={{ noWrap: true }}
                  />
                  {conv.lastMessageTime && (
                    <Typography variant="caption" color="text.secondary">
                      {formatTime(conv.lastMessageTime)}
                    </Typography>
                  )}
                </ListItemButton>
              );
            })
          )}
        </List>
      )}

      {/* 친구 목록 - 별도 컴포넌트로 분리 예정 */}
      {sidebarTab === 'friends' && (
        <Box sx={{ p: 3, textAlign: 'center' }}>
          <Typography variant="body2" color="text.secondary">
            친구 기능 준비 중...
          </Typography>
        </Box>
      )}

      {/* FAB - 새 채팅 */}
      {sidebarTab === 'rooms' && (
        <Fab
          color="primary"
          size="medium"
          onClick={() => setCreateDialogOpen(true)}
          sx={{
            position: 'absolute',
            bottom: 16,
            right: 16,
          }}
        >
          <Add />
        </Fab>
      )}

      {/* 채팅방 생성 다이얼로그 */}
      <CreateRoomDialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
      />
    </Box>
  );
}
