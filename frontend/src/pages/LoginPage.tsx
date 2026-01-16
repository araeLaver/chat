import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  Link,
  Divider,
  Tab,
  Tabs,
  InputAdornment,
  IconButton,
  Alert,
} from '@mui/material';
import {
  Visibility,
  VisibilityOff,
  Chat as ChatIcon,
  PersonOutline,
} from '@mui/icons-material';
import { useAuth } from '../hooks/useAuth';
import { useAuthStore } from '../stores/authStore';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel({ children, value, index }: TabPanelProps) {
  return (
    <div hidden={value !== index}>
      {value === index && <Box pt={3}>{children}</Box>}
    </div>
  );
}

export function LoginPage() {
  const [tab, setTab] = useState(0);
  const [showPassword, setShowPassword] = useState(false);
  const { login, registerAnonymous, isLoading } = useAuth();
  const { error, clearError } = useAuthStore();

  // 이메일 로그인 폼
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  // 익명 로그인 폼
  const [anonUsername, setAnonUsername] = useState('');

  const handleEmailLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();
    await login({ email, password });
  };

  const handleAnonymousLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();
    await registerAnonymous({ username: anonUsername });
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        background: 'linear-gradient(135deg, #1976d2 0%, #42a5f5 100%)',
        py: 4,
      }}
    >
      <Container maxWidth="sm">
        <Paper
          elevation={6}
          sx={{
            p: 4,
            borderRadius: 3,
          }}
        >
          {/* 로고 */}
          <Box display="flex" flexDirection="column" alignItems="center" mb={3}>
            <ChatIcon sx={{ fontSize: 48, color: 'primary.main', mb: 1 }} />
            <Typography variant="h4" fontWeight="bold" color="primary">
              BEAM
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Privacy-first Messenger
            </Typography>
          </Box>

          {/* 에러 메시지 */}
          {error && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={clearError}>
              {error}
            </Alert>
          )}

          {/* 탭 */}
          <Tabs
            value={tab}
            onChange={(_, newValue) => {
              setTab(newValue);
              clearError();
            }}
            variant="fullWidth"
            sx={{ mb: 1 }}
          >
            <Tab label="이메일 로그인" />
            <Tab label="익명 로그인" icon={<PersonOutline />} iconPosition="start" />
          </Tabs>

          {/* 이메일 로그인 */}
          <TabPanel value={tab} index={0}>
            <Box component="form" onSubmit={handleEmailLogin}>
              <TextField
                fullWidth
                label="이메일"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                margin="normal"
                required
                autoComplete="email"
                autoFocus
              />
              <TextField
                fullWidth
                label="비밀번호"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                margin="normal"
                required
                autoComplete="current-password"
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setShowPassword(!showPassword)}
                        edge="end"
                      >
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
              <Button
                type="submit"
                fullWidth
                variant="contained"
                size="large"
                disabled={isLoading}
                sx={{ mt: 3, mb: 2 }}
              >
                {isLoading ? '로그인 중...' : '로그인'}
              </Button>
            </Box>
          </TabPanel>

          {/* 익명 로그인 */}
          <TabPanel value={tab} index={1}>
            <Alert severity="info" sx={{ mb: 2 }}>
              익명 로그인은 빠르게 시작할 수 있지만, 나중에 계정 복구가 어려울 수 있습니다.
            </Alert>
            <Box component="form" onSubmit={handleAnonymousLogin}>
              <TextField
                fullWidth
                label="닉네임"
                value={anonUsername}
                onChange={(e) => setAnonUsername(e.target.value)}
                margin="normal"
                required
                placeholder="사용할 닉네임을 입력하세요"
                autoFocus
              />
              <Button
                type="submit"
                fullWidth
                variant="contained"
                size="large"
                disabled={isLoading}
                sx={{ mt: 3, mb: 2 }}
              >
                {isLoading ? '로그인 중...' : '익명으로 시작하기'}
              </Button>
            </Box>
          </TabPanel>

          <Divider sx={{ my: 2 }}>
            <Typography variant="body2" color="text.secondary">
              또는
            </Typography>
          </Divider>

          {/* 회원가입 링크 */}
          <Typography variant="body2" textAlign="center">
            계정이 없으신가요?{' '}
            <Link component={RouterLink} to="/register" fontWeight="bold">
              회원가입
            </Link>
          </Typography>
        </Paper>

        {/* 하단 정보 */}
        <Typography
          variant="body2"
          color="white"
          textAlign="center"
          sx={{ mt: 3, opacity: 0.9 }}
        >
          AI 통역 + 프라이버시 중심 메신저
        </Typography>
      </Container>
    </Box>
  );
}
