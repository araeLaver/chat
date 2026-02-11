import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
} from '@mui/material';
import { ArrowBack } from '@mui/icons-material';
import { roomsApi } from '../../api/rooms';
import { useAuth } from '../../contexts/AuthContext';
import { useWebSocket } from '../../contexts/WebSocketContext';

export default function CreateRoomPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { sendMessage: wsSend } = useWebSocket();
  const [form, setForm] = useState({
    roomName: '',
    description: '',
    roomType: 'PUBLIC' as 'PUBLIC' | 'PRIVATE',
    maxMembers: 100,
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (field: string) => (e: any) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.roomName.trim()) {
      setError('채팅방 이름을 입력해주세요.');
      return;
    }
    if (form.roomName.length < 2) {
      setError('채팅방 이름은 2자 이상이어야 합니다.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      // Create via WebSocket
      if (user) {
        wsSend({
          type: 'createRoom',
          roomName: form.roomName.trim(),
          creator: user.username,
          description: form.description.trim(),
          roomType: form.roomType,
        });
      }
      // Also create via REST API as fallback
      await roomsApi.createRoom({
        roomName: form.roomName.trim(),
        description: form.description.trim(),
        roomType: form.roomType,
        maxMembers: form.maxMembers,
      });
      navigate('/chats');
    } catch (err: any) {
      setError(err.response?.data?.message || err.response?.data || '채팅방 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <IconButton edge="start" onClick={() => navigate(-1)} sx={{ mr: 1 }}>
            <ArrowBack />
          </IconButton>
          <Typography variant="h6">새 채팅방</Typography>
        </Toolbar>
      </AppBar>

      <Box component="form" onSubmit={handleSubmit} sx={{ p: 3 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <TextField
          fullWidth
          label="채팅방 이름"
          value={form.roomName}
          onChange={handleChange('roomName')}
          margin="normal"
          required
          autoFocus
          inputProps={{ maxLength: 50 }}
        />

        <TextField
          fullWidth
          label="설명 (선택)"
          value={form.description}
          onChange={handleChange('description')}
          margin="normal"
          multiline
          rows={3}
          inputProps={{ maxLength: 200 }}
        />

        <FormControl fullWidth margin="normal">
          <InputLabel>공개 설정</InputLabel>
          <Select value={form.roomType} onChange={handleChange('roomType')} label="공개 설정">
            <MenuItem value="PUBLIC">공개</MenuItem>
            <MenuItem value="PRIVATE">비공개</MenuItem>
          </Select>
        </FormControl>

        <TextField
          fullWidth
          label="최대 인원"
          type="number"
          value={form.maxMembers}
          onChange={handleChange('maxMembers')}
          margin="normal"
          inputProps={{ min: 2, max: 1000 }}
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
          {loading ? '생성 중...' : '채팅방 만들기'}
        </Button>
      </Box>
    </Box>
  );
}
