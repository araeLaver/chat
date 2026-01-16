import { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  FormControlLabel,
  Switch,
  Box,
} from '@mui/material';
import { useWebSocket } from '../../hooks/useWebSocket';
import { useAuthStore } from '../../stores/authStore';
import { showSuccess, showError } from '../../stores/notificationStore';

interface CreateRoomDialogProps {
  open: boolean;
  onClose: () => void;
}

export function CreateRoomDialog({ open, onClose }: CreateRoomDialogProps) {
  const [roomName, setRoomName] = useState('');
  const [description, setDescription] = useState('');
  const [isPrivate, setIsPrivate] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const { send } = useWebSocket();
  const { user } = useAuthStore();

  const handleCreate = async () => {
    if (!roomName.trim()) {
      showError('채팅방 이름을 입력해주세요.');
      return;
    }

    setIsLoading(true);
    try {
      // WebSocket을 통해 채팅방 생성
      send({
        type: 'createRoom',
        sender: user?.username || 'anonymous',
        content: roomName.trim(),
        timestamp: new Date().toISOString(),
        userId: user?.id,
      });

      showSuccess('채팅방이 생성되었습니다.');
      handleClose();
    } catch (error) {
      showError('채팅방 생성에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    setRoomName('');
    setDescription('');
    setIsPrivate(false);
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>새 채팅방 만들기</DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            label="채팅방 이름"
            value={roomName}
            onChange={(e) => setRoomName(e.target.value)}
            fullWidth
            required
            autoFocus
            placeholder="예: 일반 채팅방"
          />
          <TextField
            label="설명 (선택)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            fullWidth
            multiline
            rows={2}
            placeholder="채팅방에 대한 설명을 입력하세요"
          />
          <FormControlLabel
            control={
              <Switch
                checked={isPrivate}
                onChange={(e) => setIsPrivate(e.target.checked)}
              />
            }
            label="비공개 채팅방"
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={isLoading}>
          취소
        </Button>
        <Button
          onClick={handleCreate}
          variant="contained"
          disabled={isLoading || !roomName.trim()}
        >
          {isLoading ? '생성 중...' : '만들기'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
