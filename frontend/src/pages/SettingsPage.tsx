import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  Switch,
  Divider,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  IconButton,
  AppBar,
  Toolbar,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
} from '@mui/material';
import { ArrowBack } from '@mui/icons-material';
import { useAuth } from '../hooks/useAuth';
import { useAuthStore } from '../stores/authStore';
import { authApi } from '../api';
import { showSuccess, showError } from '../stores/notificationStore';

const SUPPORTED_LANGUAGES = [
  { code: 'ko', name: '한국어' },
  { code: 'en', name: 'English' },
  { code: 'ja', name: '日本語' },
  { code: 'zh', name: '中文' },
  { code: 'es', name: 'Español' },
  { code: 'fr', name: 'Français' },
  { code: 'de', name: 'Deutsch' },
];

export function SettingsPage() {
  const navigate = useNavigate();
  const { user, updateProfile } = useAuth();
  const { themeMode, toggleTheme } = useAuthStore();

  const [isLoading, setIsLoading] = useState(false);

  // 프로필 설정
  const [username, setUsername] = useState(user?.username || '');

  // 프라이버시 설정
  const [ghostMode, setGhostMode] = useState(user?.ghostMode || false);
  const [disableIpLogging, setDisableIpLogging] = useState(false);

  // 번역 설정
  const [autoTranslate, setAutoTranslate] = useState(user?.autoTranslate || false);
  const [preferredLanguage, setPreferredLanguage] = useState(user?.preferredLanguage || 'ko');

  // 초기 설정 로드
  useEffect(() => {
    const loadSettings = async () => {
      try {
        const [privacySettings, translationSettings] = await Promise.all([
          authApi.getPrivacySettings(),
          authApi.getTranslationSettings(),
        ]);

        setGhostMode(privacySettings.ghostMode);
        setDisableIpLogging(privacySettings.disableIpLogging);
        setAutoTranslate(translationSettings.autoTranslate);
        setPreferredLanguage(translationSettings.preferredLanguage || 'ko');
      } catch (error) {
        console.error('Failed to load settings:', error);
      }
    };

    loadSettings();
  }, []);

  const handleSaveProfile = async () => {
    setIsLoading(true);
    try {
      await updateProfile({ username });
      showSuccess('프로필이 저장되었습니다.');
    } catch (error) {
      showError('프로필 저장에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSavePrivacy = async () => {
    setIsLoading(true);
    try {
      await authApi.updatePrivacySettings({ ghostMode, disableIpLogging });
      showSuccess('프라이버시 설정이 저장되었습니다.');
    } catch (error) {
      showError('설정 저장에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveTranslation = async () => {
    setIsLoading(true);
    try {
      await authApi.updateTranslationSettings({ autoTranslate, preferredLanguage });
      showSuccess('번역 설정이 저장되었습니다.');
    } catch (error) {
      showError('설정 저장에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="static">
        <Toolbar>
          <IconButton
            edge="start"
            color="inherit"
            onClick={() => navigate('/chat')}
            sx={{ mr: 2 }}
          >
            <ArrowBack />
          </IconButton>
          <Typography variant="h6">설정</Typography>
        </Toolbar>
      </AppBar>

      <Container maxWidth="md" sx={{ py: 3 }}>
        {/* 프로필 설정 */}
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            프로필
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <TextField
            label="사용자 이름"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            fullWidth
            sx={{ mb: 2 }}
          />

          <TextField
            label="이메일"
            value={user?.email || ''}
            fullWidth
            disabled
            sx={{ mb: 2 }}
          />

          <Button
            variant="contained"
            onClick={handleSaveProfile}
            disabled={isLoading}
          >
            프로필 저장
          </Button>
        </Paper>

        {/* 테마 설정 */}
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            테마
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <List disablePadding>
            <ListItem disablePadding>
              <ListItemText
                primary="다크 모드"
                secondary="어두운 테마를 사용합니다"
              />
              <ListItemSecondaryAction>
                <Switch
                  checked={themeMode === 'dark'}
                  onChange={toggleTheme}
                />
              </ListItemSecondaryAction>
            </ListItem>
          </List>
        </Paper>

        {/* 프라이버시 설정 */}
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            프라이버시
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <List disablePadding>
            <ListItem disablePadding sx={{ mb: 2 }}>
              <ListItemText
                primary="고스트 모드"
                secondary="다른 사용자에게 온라인 상태를 숨깁니다"
              />
              <ListItemSecondaryAction>
                <Switch
                  checked={ghostMode}
                  onChange={(e) => setGhostMode(e.target.checked)}
                />
              </ListItemSecondaryAction>
            </ListItem>

            <ListItem disablePadding>
              <ListItemText
                primary="IP 로깅 비활성화"
                secondary="보안을 위해 IP 주소 기록을 비활성화합니다"
              />
              <ListItemSecondaryAction>
                <Switch
                  checked={disableIpLogging}
                  onChange={(e) => setDisableIpLogging(e.target.checked)}
                />
              </ListItemSecondaryAction>
            </ListItem>
          </List>

          <Button
            variant="contained"
            onClick={handleSavePrivacy}
            disabled={isLoading}
            sx={{ mt: 2 }}
          >
            프라이버시 설정 저장
          </Button>
        </Paper>

        {/* 번역 설정 */}
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            번역
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <List disablePadding>
            <ListItem disablePadding sx={{ mb: 2 }}>
              <ListItemText
                primary="자동 번역"
                secondary="다른 언어의 메시지를 자동으로 번역합니다"
              />
              <ListItemSecondaryAction>
                <Switch
                  checked={autoTranslate}
                  onChange={(e) => setAutoTranslate(e.target.checked)}
                />
              </ListItemSecondaryAction>
            </ListItem>
          </List>

          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>선호 언어</InputLabel>
            <Select
              value={preferredLanguage}
              onChange={(e) => setPreferredLanguage(e.target.value)}
              label="선호 언어"
            >
              {SUPPORTED_LANGUAGES.map((lang) => (
                <MenuItem key={lang.code} value={lang.code}>
                  {lang.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <Button
            variant="contained"
            onClick={handleSaveTranslation}
            disabled={isLoading}
          >
            번역 설정 저장
          </Button>
        </Paper>
      </Container>
    </Box>
  );
}
