import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Divider,
  Button,
  Paper,
} from '@mui/material';
import {
  Person,
  Notifications,
  Storage,
  Info,
  ExitToApp,
} from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { useWebSocket } from '../../contexts/WebSocketContext';
import AvatarWithStatus from '../../components/common/AvatarWithStatus';

export default function SettingsPage() {
  const { user, logout } = useAuth();
  const { isConnected } = useWebSocket();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <Box sx={{ height: '100%' }}>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <Typography variant="h6">설정</Typography>
        </Toolbar>
      </AppBar>

      {/* Profile card */}
      {user && (
        <Paper sx={{ m: 2, p: 2.5, display: 'flex', alignItems: 'center', gap: 2 }}>
          <AvatarWithStatus name={user.displayName || user.username} size={56} online />
          <Box sx={{ flex: 1 }}>
            <Typography variant="h6" fontWeight={600}>
              {user.displayName || user.username}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              @{user.username}
            </Typography>
            {user.phoneNumber && (
              <Typography variant="caption" color="text.secondary">
                {user.phoneNumber}
              </Typography>
            )}
          </Box>
        </Paper>
      )}

      {/* Connection status */}
      <Box sx={{ px: 2, mb: 1 }}>
        <Typography
          variant="caption"
          sx={{
            color: isConnected ? 'success.main' : 'error.main',
            display: 'flex',
            alignItems: 'center',
            gap: 0.5,
          }}
        >
          <Box
            component="span"
            sx={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              bgcolor: isConnected ? 'success.main' : 'error.main',
              display: 'inline-block',
            }}
          />
          {isConnected ? '서버 연결됨' : '서버 연결 끊김'}
        </Typography>
      </Box>

      <List>
        <ListItem>
          <ListItemIcon>
            <Person />
          </ListItemIcon>
          <ListItemText primary="프로필 관리" secondary="이름, 상태메시지 변경" />
        </ListItem>
        <Divider variant="inset" component="li" />
        <ListItem>
          <ListItemIcon>
            <Notifications />
          </ListItemIcon>
          <ListItemText primary="알림 설정" secondary="푸시 알림, 소리" />
        </ListItem>
        <Divider variant="inset" component="li" />
        <ListItem>
          <ListItemIcon>
            <Storage />
          </ListItemIcon>
          <ListItemText primary="저장공간 관리" secondary="캐시, 파일 정리" />
        </ListItem>
        <Divider variant="inset" component="li" />
        <ListItem>
          <ListItemIcon>
            <Info />
          </ListItemIcon>
          <ListItemText primary="앱 정보" secondary="BEAM Chat v1.0.0" />
        </ListItem>
      </List>

      <Box sx={{ p: 2 }}>
        <Button
          fullWidth
          variant="outlined"
          color="error"
          startIcon={<ExitToApp />}
          onClick={handleLogout}
          sx={{ py: 1.2 }}
        >
          로그아웃
        </Button>
      </Box>
    </Box>
  );
}
