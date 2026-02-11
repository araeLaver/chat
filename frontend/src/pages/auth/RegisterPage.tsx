import { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Box,
  TextField,
  Button,
  Typography,
  Paper,
  Alert,
  IconButton,
  InputAdornment,
} from '@mui/material';
import { Visibility, VisibilityOff, ArrowBack } from '@mui/icons-material';
import { useAuth } from '../../contexts/AuthContext';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [form, setForm] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    displayName: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const validate = (): string | null => {
    if (!form.username.trim()) return '아이디를 입력해주세요.';
    if (form.username.length < 3 || form.username.length > 20)
      return '아이디는 3~20자로 입력해주세요.';
    if (!form.displayName.trim()) return '닉네임을 입력해주세요.';
    if (!form.password) return '비밀번호를 입력해주세요.';
    if (form.password.length < 8) return '비밀번호는 8자 이상이어야 합니다.';
    if (!/[A-Z]/.test(form.password)) return '비밀번호에 대문자를 포함해주세요.';
    if (!/[a-z]/.test(form.password)) return '비밀번호에 소문자를 포함해주세요.';
    if (!/[0-9]/.test(form.password)) return '비밀번호에 숫자를 포함해주세요.';
    if (!/[@$!%*?&]/.test(form.password)) return '비밀번호에 특수문자(@$!%*?&)를 포함해주세요.';
    if (form.password !== form.confirmPassword) return '비밀번호가 일치하지 않습니다.';
    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setLoading(true);
    setError('');
    try {
      await register(form.username, form.password, form.displayName);
      navigate('/friends');
    } catch (err: any) {
      setError(err.response?.data?.message || err.response?.data || '회원가입에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Paper elevation={0} sx={{ p: 4, borderRadius: 3, bgcolor: 'rgba(255,255,255,0.95)' }}>
      <Box display="flex" alignItems="center" mb={3}>
        <IconButton component={RouterLink} to="/login" sx={{ mr: 1 }}>
          <ArrowBack />
        </IconButton>
        <Typography variant="h5" fontWeight={700} color="#3C1E1E">
          회원가입
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Box component="form" onSubmit={handleSubmit}>
        <TextField
          fullWidth
          label="아이디"
          value={form.username}
          onChange={handleChange('username')}
          margin="normal"
          helperText="3~20자, 영문/숫자/밑줄"
          autoFocus
        />
        <TextField
          fullWidth
          label="닉네임"
          value={form.displayName}
          onChange={handleChange('displayName')}
          margin="normal"
        />
        <TextField
          fullWidth
          label="비밀번호"
          type={showPassword ? 'text' : 'password'}
          value={form.password}
          onChange={handleChange('password')}
          margin="normal"
          helperText="8자 이상, 대/소문자, 숫자, 특수문자 포함"
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
        <TextField
          fullWidth
          label="비밀번호 확인"
          type="password"
          value={form.confirmPassword}
          onChange={handleChange('confirmPassword')}
          margin="normal"
        />
        <Button
          fullWidth
          type="submit"
          variant="contained"
          color="primary"
          size="large"
          disabled={loading}
          sx={{ mt: 3, py: 1.5 }}
        >
          {loading ? '가입 중...' : '가입하기'}
        </Button>
      </Box>
    </Paper>
  );
}
