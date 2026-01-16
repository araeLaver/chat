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
  InputAdornment,
  IconButton,
  Alert,
} from '@mui/material';
import {
  Visibility,
  VisibilityOff,
  Chat as ChatIcon,
} from '@mui/icons-material';
import { useAuth } from '../hooks/useAuth';
import { useAuthStore } from '../stores/authStore';

export function RegisterPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const { register, isLoading } = useAuth();
  const { error, clearError } = useAuthStore();

  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    phoneNumber: '',
  });

  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  const handleChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [field]: e.target.value });
    setFormErrors({ ...formErrors, [field]: '' });
  };

  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.username.trim()) {
      errors.username = '사용자 이름을 입력해주세요.';
    } else if (formData.username.length < 2) {
      errors.username = '사용자 이름은 2자 이상이어야 합니다.';
    }

    if (!formData.email.trim()) {
      errors.email = '이메일을 입력해주세요.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = '올바른 이메일 형식을 입력해주세요.';
    }

    if (!formData.password) {
      errors.password = '비밀번호를 입력해주세요.';
    } else if (formData.password.length < 6) {
      errors.password = '비밀번호는 6자 이상이어야 합니다.';
    }

    if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();

    if (!validate()) return;

    await register({
      username: formData.username,
      email: formData.email,
      password: formData.password,
      phoneNumber: formData.phoneNumber || undefined,
    });
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
              회원가입
            </Typography>
          </Box>

          {/* 에러 메시지 */}
          {error && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={clearError}>
              {error}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit}>
            <TextField
              fullWidth
              label="사용자 이름"
              value={formData.username}
              onChange={handleChange('username')}
              margin="normal"
              required
              error={!!formErrors.username}
              helperText={formErrors.username}
              autoFocus
            />

            <TextField
              fullWidth
              label="이메일"
              type="email"
              value={formData.email}
              onChange={handleChange('email')}
              margin="normal"
              required
              error={!!formErrors.email}
              helperText={formErrors.email}
              autoComplete="email"
            />

            <TextField
              fullWidth
              label="전화번호 (선택)"
              type="tel"
              value={formData.phoneNumber}
              onChange={handleChange('phoneNumber')}
              margin="normal"
              placeholder="010-0000-0000"
              helperText="친구 검색에 사용됩니다."
            />

            <TextField
              fullWidth
              label="비밀번호"
              type={showPassword ? 'text' : 'password'}
              value={formData.password}
              onChange={handleChange('password')}
              margin="normal"
              required
              error={!!formErrors.password}
              helperText={formErrors.password || '6자 이상 입력해주세요.'}
              autoComplete="new-password"
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

            <TextField
              fullWidth
              label="비밀번호 확인"
              type={showConfirmPassword ? 'text' : 'password'}
              value={formData.confirmPassword}
              onChange={handleChange('confirmPassword')}
              margin="normal"
              required
              error={!!formErrors.confirmPassword}
              helperText={formErrors.confirmPassword}
              autoComplete="new-password"
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      edge="end"
                    >
                      {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
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
              {isLoading ? '가입 중...' : '회원가입'}
            </Button>
          </Box>

          {/* 로그인 링크 */}
          <Typography variant="body2" textAlign="center" sx={{ mt: 2 }}>
            이미 계정이 있으신가요?{' '}
            <Link component={RouterLink} to="/login" fontWeight="bold">
              로그인
            </Link>
          </Typography>
        </Paper>
      </Container>
    </Box>
  );
}
