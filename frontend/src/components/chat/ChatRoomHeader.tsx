import {
  Box,
  Typography,
  IconButton,
} from '@mui/material';
import {
  MoreVert,
  Info,
  Circle,
} from '@mui/icons-material';
import type { TypingIndicator } from '../../types';

interface ChatRoomHeaderProps {
  title: string;
  subtitle?: string;
  isOnline?: boolean;
  typingUsers?: TypingIndicator[];
  onInfoClick?: () => void;
  onMoreClick?: () => void;
}

export function ChatRoomHeader({
  title,
  subtitle,
  isOnline,
  typingUsers = [],
  onInfoClick,
  onMoreClick,
}: ChatRoomHeaderProps) {
  const typingText = typingUsers.length > 0
    ? typingUsers.length === 1
      ? `${typingUsers[0].username}님이 입력 중...`
      : `${typingUsers.length}명이 입력 중...`
    : null;

  return (
    <Box
      sx={{
        px: 2,
        py: 1.5,
        borderBottom: 1,
        borderColor: 'divider',
        bgcolor: 'background.paper',
        display: 'flex',
        alignItems: 'center',
        gap: 1,
      }}
    >
      <Box sx={{ flex: 1 }}>
        <Box display="flex" alignItems="center" gap={1}>
          <Typography variant="subtitle1" fontWeight="bold">
            {title}
          </Typography>
          {isOnline !== undefined && (
            <Circle
              sx={{
                fontSize: 8,
                color: isOnline ? 'success.main' : 'text.disabled',
              }}
            />
          )}
        </Box>
        {typingText ? (
          <Typography variant="caption" color="primary" sx={{ fontStyle: 'italic' }}>
            {typingText}
          </Typography>
        ) : subtitle ? (
          <Typography variant="caption" color="text.secondary">
            {subtitle}
          </Typography>
        ) : null}
      </Box>

      <IconButton size="small" onClick={onInfoClick}>
        <Info />
      </IconButton>
      <IconButton size="small" onClick={onMoreClick}>
        <MoreVert />
      </IconButton>
    </Box>
  );
}
