import { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Box,
  TextField,
  Button,
  Typography,
  Paper,
  Divider,
  Alert,
  IconButton,
  InputAdornment,
} from '@mui/material';
import { Visibility, VisibilityOff, Chat as ChatIcon } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';

export default function LoginPage() {
  const navigate = useNavigate();
  const { login, guestLogin } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('아이디와 비밀번호를 입력해주세요.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await login(username, password);
      navigate('/friends');
    } catch (err: any) {
      setError(err.response?.data?.message || err.response?.data || '로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleGuest = async () => {
    setLoading(true);
    setError('');
    try {
      await guestLogin();
      navigate('/friends');
    } catch (err: any) {
      setError(err.response?.data?.message || '게스트 로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Paper elevation={0} sx={{ p: 4, borderRadius: 3, bgcolor: 'rgba(255,255,255,0.95)' }}>
      <Box textAlign="center" mb={3}>
        <ChatIcon sx={{ fontSize: 56, color: '#3C1E1E', mb: 1 }} />
        <Typography variant="h5" fontWeight={700} color="#3C1E1E">
          BEAM Chat
        </Typography>
        <Typography variant="body2" color="text.secondary" mt={0.5}>
          카카오톡 스타일 메신저
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Box component="form" onSubmit={handleLogin}>
        <TextField
          fullWidth
          label="아이디"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          margin="normal"
          autoComplete="username"
          autoFocus
        />
        <TextField
          fullWidth
          label="비밀번호"
          type={showPassword ? 'text' : 'password'}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          margin="normal"
          autoComplete="current-password"
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton onClick={() => setShowPassword(!showPassword)} edge="end" size="small">
                  {showPassword ? <VisibilityOff /> : <Visibility />}
                </IconButton>
              </InputAdornment>
            ),
          }}
        />
        <Button
          fullWidth
          type="submit"
          variant="contained"
          color="primary"
          size="large"
          disabled={loading}
          sx={{ mt: 2, mb: 1, py: 1.5 }}
        >
          {loading ? '로그인 중...' : '로그인'}
        </Button>
      </Box>

      <Divider sx={{ my: 2 }}>
        <Typography variant="body2" color="text.secondary">
          또는
        </Typography>
      </Divider>

      <Button
        fullWidth
        variant="outlined"
        onClick={handleGuest}
        disabled={loading}
        sx={{ mb: 1.5, color: '#3C1E1E', borderColor: '#3C1E1E' }}
      >
        게스트로 시작하기
      </Button>

      <Box textAlign="center" mt={1}>
        <Typography variant="body2" color="text.secondary">
          계정이 없으신가요?{' '}
          <Typography
            component={RouterLink}
            to="/register"
            variant="body2"
            sx={{ color: '#3C1E1E', fontWeight: 600, textDecoration: 'none' }}
          >
            회원가입
          </Typography>
        </Typography>
      </Box>
    </Paper>
  );
}
