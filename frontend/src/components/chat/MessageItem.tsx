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
  Done,
  DoneAll,
} from '@mui/icons-material';
import { useState } from 'react';
import { format } from 'date-fns';
import { useTheme } from '@mui/material/styles';
import { Avatar } from '../common/Avatar';
import { useAuthStore } from '../../stores/authStore';
import type { ChatMessage } from '../../types';

interface MessageItemProps {
  message: ChatMessage;
  onReply?: (message: ChatMessage) => void;
  showAvatar?: boolean;
  showSenderName?: boolean;
  showTime?: boolean;
  isFirstInGroup?: boolean;
  isLastInGroup?: boolean;
}

export function MessageItem({
  message,
  onReply,
  showAvatar = true,
  showSenderName = true,
  showTime = true,
  isFirstInGroup = true,
  isLastInGroup = true,
}: MessageItemProps) {
  const theme = useTheme();
  const { user } = useAuthStore();
  const [showTranslation, setShowTranslation] = useState(true);

  const isOwnMessage = message.sender === user?.username;

  // 테마에서 채팅 색상 가져오기 (커스텀 팔레트 지원)
  const chatColors = (theme.palette as any).chat || {
    bubbleOwn: theme.palette.primary.main,
    bubbleOther: theme.palette.mode === 'dark' ? '#2D2D2D' : '#FFFFFF',
  };
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

  // 아바타 공간 확보를 위한 너비 (32px 아바타 + 8px gap)
  const avatarSpaceWidth = 40;

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: isOwnMessage ? 'row-reverse' : 'row',
        gap: 1,
        mb: isLastInGroup ? 1 : 0.25,
        mt: isFirstInGroup ? 0.5 : 0,
      }}
    >
      {/* 아바타 또는 공간 유지 */}
      {!isOwnMessage && (
        showAvatar ? (
          <Avatar name={message.sender} size="small" />
        ) : (
          <Box sx={{ width: avatarSpaceWidth - 8, flexShrink: 0 }} />
        )
      )}

      <Box
        sx={{
          maxWidth: '70%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: isOwnMessage ? 'flex-end' : 'flex-start',
        }}
      >
        {/* 발신자 이름 - 그룹의 첫 메시지에만 표시 */}
        {!isOwnMessage && showSenderName && (
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
            bgcolor: isOwnMessage ? chatColors.bubbleOwn : chatColors.bubbleOther,
            color: isOwnMessage ? theme.palette.primary.contrastText : theme.palette.text.primary,
            borderRadius: 2,
            borderTopRightRadius: isOwnMessage ? 0 : 2,
            borderTopLeftRadius: isOwnMessage ? 2 : 0,
            boxShadow: theme.palette.mode === 'light' ? '0 1px 2px rgba(0,0,0,0.1)' : 'none',
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

        {/* 메타 정보 - 그룹의 마지막 메시지에만 표시 */}
        {showTime && (
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

            {/* 읽음 표시 - 체크 아이콘으로 변경 */}
            {isOwnMessage && (
              message.isRead ? (
                <Tooltip title="읽음">
                  <DoneAll sx={{ fontSize: 14, color: 'primary.main' }} />
                </Tooltip>
              ) : (
                <Tooltip title="전송됨">
                  <Done sx={{ fontSize: 14, color: 'text.disabled' }} />
                </Tooltip>
              )
            )}
          </Box>
        )}

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
