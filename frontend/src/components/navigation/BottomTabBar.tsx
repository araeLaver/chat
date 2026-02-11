import { useLocation, useNavigate } from 'react-router-dom';
import { BottomNavigation, BottomNavigationAction, Badge, Paper } from '@mui/material';
import {
  People as FriendsIcon,
  ChatBubble as ChatIcon,
  Search as SearchIcon,
  Settings as SettingsIcon,
} from '@mui/icons-material';
import { useChat } from '../../contexts/ChatContext';

export default function BottomTabBar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { totalUnread } = useChat();

  const getTab = () => {
    if (location.pathname.startsWith('/friends')) return 0;
    if (location.pathname.startsWith('/chats') || location.pathname.startsWith('/dm') || location.pathname.startsWith('/room')) return 1;
    if (location.pathname.startsWith('/search')) return 2;
    if (location.pathname.startsWith('/settings')) return 3;
    return 0;
  };

  const handleChange = (_: React.SyntheticEvent, value: number) => {
    const routes = ['/friends', '/chats', '/search', '/settings'];
    navigate(routes[value]);
  };

  return (
    <Paper
      sx={{ position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: 1200 }}
      elevation={3}
    >
      <BottomNavigation value={getTab()} onChange={handleChange} showLabels>
        <BottomNavigationAction label="친구" icon={<FriendsIcon />} />
        <BottomNavigationAction
          label="채팅"
          icon={
            <Badge badgeContent={totalUnread} color="error" max={999}>
              <ChatIcon />
            </Badge>
          }
        />
        <BottomNavigationAction label="검색" icon={<SearchIcon />} />
        <BottomNavigationAction label="설정" icon={<SettingsIcon />} />
      </BottomNavigation>
    </Paper>
  );
}
