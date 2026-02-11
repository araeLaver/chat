import { useState, useRef } from 'react';
import { Box, TextField, IconButton, CircularProgress } from '@mui/material';
import { Send, AttachFile, Image as ImageIcon } from '@mui/icons-material';
import { MAX_FILE_SIZE } from '../../utils/constants';

interface MessageInputProps {
  onSend: (content: string) => void;
  onFileUpload?: (file: File) => void;
  disabled?: boolean;
  placeholder?: string;
}

export default function MessageInput({ onSend, onFileUpload, disabled, placeholder }: MessageInputProps) {
  const [text, setText] = useState('');
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleSend = () => {
    const trimmed = text.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setText('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !onFileUpload) return;
    if (file.size > MAX_FILE_SIZE) {
      alert('파일 크기는 10MB 이하만 가능합니다.');
      return;
    }
    setUploading(true);
    try {
      await onFileUpload(file);
    } finally {
      setUploading(false);
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'flex-end',
        gap: 0.5,
        p: 1,
        bgcolor: '#FFFFFF',
        borderTop: '1px solid #E0E0E0',
      }}
    >
      {onFileUpload && (
        <>
          <IconButton size="small" onClick={() => fileRef.current?.click()} disabled={uploading}>
            {uploading ? <CircularProgress size={20} /> : <AttachFile />}
          </IconButton>
          <input ref={fileRef} type="file" hidden onChange={handleFileChange} />
        </>
      )}
      <TextField
        fullWidth
        multiline
        maxRows={4}
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder || '메시지를 입력하세요'}
        variant="outlined"
        size="small"
        disabled={disabled}
        sx={{
          '& .MuiOutlinedInput-root': {
            borderRadius: 3,
            bgcolor: '#F5F5F5',
          },
        }}
      />
      <IconButton
        color="primary"
        onClick={handleSend}
        disabled={!text.trim() || disabled}
        sx={{
          bgcolor: text.trim() ? '#FEE500' : 'transparent',
          '&:hover': { bgcolor: text.trim() ? '#F5D800' : undefined },
        }}
      >
        <Send sx={{ color: text.trim() ? '#3C1E1E' : '#BDBDBD' }} />
      </IconButton>
    </Box>
  );
}
