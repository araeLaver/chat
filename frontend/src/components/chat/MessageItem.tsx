import {
  Box,
  Typography,
  Paper,
  IconButton,
  Tooltip,
  Chip,
} from '@mui/material';
import {
  Reply,
  InsertDriveFile,
  Timer,
  Translate,
} from '@mui/icons-material';
import { useState } from 'react';
import { format } from 'date-fns';
import { Avatar } from '../common/Avatar';
import { useAuthStore } from '../../stores/authStore';
import type { ChatMessage } from '../../types';

interface MessageItemProps {
  message: ChatMessage;
  onReply?: (message: ChatMessage) => void;
}

export function MessageItem({ message, onReply }: MessageItemProps) {
  const { user } = useAuthStore();
  const [showTranslation, setShowTranslation] = useState(true);

  const isOwnMessage = message.sender === user?.username;
  const isSystemMessage = message.type === 'system';

  // 시스템 메시지
  if (isSystemMessage) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          my: 1,
        }}
      >
        <Chip
          label={message.content}
          size="small"
          variant="outlined"
          sx={{ bgcolor: 'background.paper' }}
        />
      </Box>
    );
  }

  const timeStr = format(new Date(message.timestamp), 'HH:mm');
  const hasTranslation = message.translatedContent && message.translatedContent !== message.content;
  const isFile = message.type === 'file' || message.type === 'image';
  const isImage = message.type === 'image' || message.mimeType?.startsWith('image/');
  const hasTTL = message.ttlSeconds && message.ttlSeconds > 0;

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: isOwnMessage ? 'row-reverse' : 'row',
        gap: 1,
        mb: 1,
      }}
    >
      {/* 아바타 */}
      {!isOwnMessage && (
        <Avatar name={message.sender} size="small" />
      )}

      <Box
        sx={{
          maxWidth: '70%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: isOwnMessage ? 'flex-end' : 'flex-start',
        }}
      >
        {/* 발신자 이름 */}
        {!isOwnMessage && (
          <Typography variant="caption" color="text.secondary" sx={{ ml: 1, mb: 0.5 }}>
            {message.sender}
          </Typography>
        )}

        {/* 답장 표시 */}
        {message.replyToId && (
          <Paper
            sx={{
              px: 1.5,
              py: 0.5,
              mb: 0.5,
              bgcolor: 'action.hover',
              borderLeft: 3,
              borderColor: 'primary.main',
            }}
          >
            <Typography variant="caption" color="primary" fontWeight="bold">
              {message.replyToSender}
            </Typography>
            <Typography variant="caption" display="block" noWrap>
              {message.replyToContent}
            </Typography>
          </Paper>
        )}

        {/* 메시지 본문 */}
        <Paper
          elevation={0}
          sx={{
            px: 2,
            py: 1,
            bgcolor: isOwnMessage ? 'primary.main' : 'grey.100',
            color: isOwnMessage ? 'primary.contrastText' : 'text.primary',
            borderRadius: 2,
            borderTopRightRadius: isOwnMessage ? 0 : 2,
            borderTopLeftRadius: isOwnMessage ? 2 : 0,
          }}
        >
          {/* 이미지 */}
          {isImage && message.fileUrl && (
            <Box
              component="img"
              src={message.fileUrl}
              alt={message.fileName || 'Image'}
              sx={{
                maxWidth: '100%',
                maxHeight: 300,
                borderRadius: 1,
                mb: message.content ? 1 : 0,
              }}
            />
          )}

          {/* 파일 */}
          {isFile && !isImage && (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                p: 1,
                bgcolor: 'rgba(0,0,0,0.1)',
                borderRadius: 1,
                mb: message.content ? 1 : 0,
              }}
            >
              <InsertDriveFile />
              <Box>
                <Typography variant="body2" fontWeight="bold">
                  {message.fileName}
                </Typography>
                {message.fileSize && (
                  <Typography variant="caption">
                    {(message.fileSize / 1024).toFixed(1)} KB
                  </Typography>
                )}
              </Box>
            </Box>
          )}

          {/* 텍스트 내용 */}
          {message.content && (
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
              {showTranslation && hasTranslation ? message.translatedContent : message.content}
            </Typography>
          )}

          {/* 번역 토글 */}
          {hasTranslation && (
            <Box sx={{ mt: 1 }}>
              <Chip
                icon={<Translate fontSize="small" />}
                label={showTranslation ? '원본 보기' : '번역 보기'}
                size="small"
                onClick={() => setShowTranslation(!showTranslation)}
                sx={{
                  height: 24,
                  cursor: 'pointer',
                  bgcolor: 'rgba(0,0,0,0.1)',
                }}
              />
            </Box>
          )}
        </Paper>

        {/* 메타 정보 */}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 0.5,
            mt: 0.5,
            px: 0.5,
          }}
        >
          {/* TTL 표시 */}
          {hasTTL && (
            <Tooltip title={`${message.ttlSeconds}초 후 삭제`}>
              <Timer sx={{ fontSize: 12, color: 'warning.main' }} />
            </Tooltip>
          )}

          {/* 시간 */}
          <Typography variant="caption" color="text.secondary">
            {timeStr}
          </Typography>

          {/* 읽음 표시 */}
          {isOwnMessage && message.isRead && (
            <Typography variant="caption" color="primary">
              읽음
            </Typography>
          )}
        </Box>

        {/* 리액션 */}
        {message.reactions && message.reactions.length > 0 && (
          <Box sx={{ display: 'flex', gap: 0.5, mt: 0.5 }}>
            {message.reactions.map((reaction) => (
              <Chip
                key={reaction.id}
                label={`${reaction.emoji} ${message.reactions?.filter(r => r.emoji === reaction.emoji).length}`}
                size="small"
                sx={{ height: 24 }}
              />
            ))}
          </Box>
        )}
      </Box>

      {/* 액션 버튼 */}
      <Box
        sx={{
          opacity: 0,
          transition: 'opacity 0.2s',
          '&:hover': { opacity: 1 },
          alignSelf: 'center',
        }}
      >
        <IconButton size="small" onClick={() => onReply?.(message)}>
          <Reply fontSize="small" />
        </IconButton>
      </Box>
    </Box>
  );
}
