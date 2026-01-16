import { useState, useRef, type KeyboardEvent } from 'react';
import {
  Box,
  TextField,
  IconButton,
  Tooltip,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Chip,
  Typography,
} from '@mui/material';
import {
  Send,
  AttachFile,
  Timer,
  Close,
  Image as ImageIcon,
  InsertDriveFile,
} from '@mui/icons-material';

interface MessageInputProps {
  onSendMessage: (content: string, options?: {
    ttlSeconds?: number;
    replyToId?: number;
    mentions?: string[];
  }) => void;
  onSendFile: (file: File) => void;
  onTyping: () => void;
  disabled?: boolean;
  replyTo?: {
    id: number;
    sender: string;
    content: string;
  };
  onCancelReply?: () => void;
}

const TTL_OPTIONS = [
  { label: '없음', value: 0 },
  { label: '30초', value: 30 },
  { label: '1분', value: 60 },
  { label: '5분', value: 300 },
  { label: '1시간', value: 3600 },
  { label: '24시간', value: 86400 },
];

export function MessageInput({
  onSendMessage,
  onSendFile,
  onTyping,
  disabled = false,
  replyTo,
  onCancelReply,
}: MessageInputProps) {
  const [message, setMessage] = useState('');
  const [ttlSeconds, setTtlSeconds] = useState(0);
  const [ttlMenuAnchor, setTtlMenuAnchor] = useState<null | HTMLElement>(null);
  const [attachMenuAnchor, setAttachMenuAnchor] = useState<null | HTMLElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);

  const handleSend = () => {
    const trimmed = message.trim();
    if (!trimmed) return;

    onSendMessage(trimmed, {
      ttlSeconds: ttlSeconds > 0 ? ttlSeconds : undefined,
      replyToId: replyTo?.id,
    });

    setMessage('');
    setTtlSeconds(0);
    onCancelReply?.();
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setMessage(e.target.value);
    onTyping();
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onSendFile(file);
    }
    e.target.value = '';
    setAttachMenuAnchor(null);
  };

  const handleTtlSelect = (seconds: number) => {
    setTtlSeconds(seconds);
    setTtlMenuAnchor(null);
  };

  const ttlLabel = TTL_OPTIONS.find(opt => opt.value === ttlSeconds)?.label || '없음';

  return (
    <Box sx={{ p: 1 }}>
      {/* 답장 표시 */}
      {replyTo && (
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1,
            mb: 1,
            p: 1,
            bgcolor: 'action.hover',
            borderRadius: 1,
            borderLeft: 3,
            borderColor: 'primary.main',
          }}
        >
          <Box sx={{ flex: 1 }}>
            <Typography variant="caption" color="primary" fontWeight="bold">
              {replyTo.sender}에게 답장
            </Typography>
            <Typography variant="caption" display="block" noWrap>
              {replyTo.content}
            </Typography>
          </Box>
          <IconButton size="small" onClick={onCancelReply}>
            <Close fontSize="small" />
          </IconButton>
        </Box>
      )}

      {/* TTL 표시 */}
      {ttlSeconds > 0 && (
        <Chip
          icon={<Timer fontSize="small" />}
          label={`${ttlLabel} 후 삭제`}
          size="small"
          color="warning"
          onDelete={() => setTtlSeconds(0)}
          sx={{ mb: 1 }}
        />
      )}

      {/* 입력 영역 */}
      <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 1 }}>
        {/* 첨부 버튼 */}
        <Tooltip title="파일 첨부">
          <IconButton
            onClick={(e) => setAttachMenuAnchor(e.currentTarget)}
            disabled={disabled}
          >
            <AttachFile />
          </IconButton>
        </Tooltip>

        {/* TTL 버튼 */}
        <Tooltip title="자동 삭제 설정">
          <IconButton
            onClick={(e) => setTtlMenuAnchor(e.currentTarget)}
            disabled={disabled}
            color={ttlSeconds > 0 ? 'warning' : 'default'}
          >
            <Timer />
          </IconButton>
        </Tooltip>

        {/* 입력 필드 */}
        <TextField
          fullWidth
          multiline
          maxRows={4}
          placeholder="메시지를 입력하세요..."
          value={message}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          size="small"
          sx={{
            '& .MuiOutlinedInput-root': {
              borderRadius: 3,
            },
          }}
        />

        {/* 전송 버튼 */}
        <Tooltip title="전송 (Enter)">
          <span>
            <IconButton
              color="primary"
              onClick={handleSend}
              disabled={disabled || !message.trim()}
            >
              <Send />
            </IconButton>
          </span>
        </Tooltip>
      </Box>

      {/* 첨부 메뉴 */}
      <Menu
        anchorEl={attachMenuAnchor}
        open={Boolean(attachMenuAnchor)}
        onClose={() => setAttachMenuAnchor(null)}
      >
        <MenuItem onClick={() => imageInputRef.current?.click()}>
          <ListItemIcon>
            <ImageIcon />
          </ListItemIcon>
          <ListItemText>이미지</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => fileInputRef.current?.click()}>
          <ListItemIcon>
            <InsertDriveFile />
          </ListItemIcon>
          <ListItemText>파일</ListItemText>
        </MenuItem>
      </Menu>

      {/* TTL 메뉴 */}
      <Menu
        anchorEl={ttlMenuAnchor}
        open={Boolean(ttlMenuAnchor)}
        onClose={() => setTtlMenuAnchor(null)}
      >
        {TTL_OPTIONS.map((option) => (
          <MenuItem
            key={option.value}
            onClick={() => handleTtlSelect(option.value)}
            selected={ttlSeconds === option.value}
          >
            {option.label}
          </MenuItem>
        ))}
      </Menu>

      {/* 숨겨진 파일 입력 */}
      <input
        ref={fileInputRef}
        type="file"
        hidden
        onChange={handleFileSelect}
      />
      <input
        ref={imageInputRef}
        type="file"
        accept="image/*"
        hidden
        onChange={handleFileSelect}
      />
    </Box>
  );
}

