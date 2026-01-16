import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Box,
  Avatar,
  Menu,
  MenuItem,
  Divider,
  Chip,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Settings,
  Logout,
  DarkMode,
  LightMode,
  Wifi,
  WifiOff,
} from '@mui/icons-material';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useAuthStore } from '../../stores/authStore';

interface ChatHeaderProps {
  onMenuClick: () => void;
  isConnected: boolean;
}

export function ChatHeader({ onMenuClick, isConnected }: ChatHeaderProps) {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { themeMode, toggleTheme } = useAuthStore();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleSettings = () => {
    handleMenuClose();
    navigate('/settings');
  };

  const handleLogout = () => {
    handleMenuClose();
    logout();
  };

  return (
    <AppBar
      position="fixed"
      sx={{
        zIndex: (theme) => theme.zIndex.drawer + 1,
      }}
    >
      <Toolbar>
        <IconButton
          color="inherit"
          edge="start"
          onClick={onMenuClick}
          sx={{ mr: 2, display: { md: 'none' } }}
        >
          <MenuIcon />
        </IconButton>

        <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 0 }}>
          BEAM
        </Typography>

        {/* 연결 상태 */}
        <Chip
          icon={isConnected ? <Wifi fontSize="small" /> : <WifiOff fontSize="small" />}
          label={isConnected ? '연결됨' : '연결 끊김'}
          size="small"
          color={isConnected ? 'success' : 'error'}
          variant="outlined"
          sx={{ ml: 2 }}
        />

        <Box sx={{ flexGrow: 1 }} />

        {/* 테마 토글 */}
        <IconButton color="inherit" onClick={toggleTheme}>
          {themeMode === 'dark' ? <LightMode /> : <DarkMode />}
        </IconButton>

        {/* 사용자 메뉴 */}
        <IconButton
          onClick={handleMenuOpen}
          sx={{ ml: 1 }}
        >
          <Avatar
            sx={{ width: 32, height: 32, bgcolor: 'secondary.main' }}
          >
            {user?.username?.[0]?.toUpperCase() || '?'}
          </Avatar>
        </IconButton>

        <Menu
          anchorEl={anchorEl}
          open={Boolean(anchorEl)}
          onClose={handleMenuClose}
          PaperProps={{
            sx: { minWidth: 200, mt: 1 },
          }}
          transformOrigin={{ horizontal: 'right', vertical: 'top' }}
          anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        >
          <Box sx={{ px: 2, py: 1 }}>
            <Typography variant="subtitle1" fontWeight="bold">
              {user?.username}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {user?.email || '익명 사용자'}
            </Typography>
          </Box>
          <Divider />
          <MenuItem onClick={handleSettings}>
            <Settings fontSize="small" sx={{ mr: 1 }} />
            설정
          </MenuItem>
          <MenuItem onClick={handleLogout}>
            <Logout fontSize="small" sx={{ mr: 1 }} />
            로그아웃
          </MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );
}
