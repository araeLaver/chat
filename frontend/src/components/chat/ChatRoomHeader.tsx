import {
  Box,
  Typography,
  IconButton,
  keyframes,
} from '@mui/material';
import {
  MoreVert,
  Info,
  Circle,
} from '@mui/icons-material';
import type { TypingIndicator } from '../../types';

// 타이핑 점 애니메이션
const bounce = keyframes`
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-4px);
    opacity: 1;
  }
`;

function TypingDots() {
  return (
    <Box
      component="span"
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '3px',
        ml: 0.5,
      }}
    >
      {[0, 1, 2].map((index) => (
        <Box
          key={index}
          component="span"
          sx={{
            width: 5,
            height: 5,
            borderRadius: '50%',
            bgcolor: 'primary.main',
            display: 'inline-block',
            animation: `${bounce} 1.4s ease-in-out infinite`,
            animationDelay: `${index * 0.16}s`,
          }}
        />
      ))}
    </Box>
  );
}

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
  const typingUserName = typingUsers.length > 0
    ? typingUsers.length === 1
      ? typingUsers[0].username
      : `${typingUsers.length}명`
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
        {typingUserName ? (
          <Typography
            variant="caption"
            color="primary"
            sx={{
              display: 'flex',
              alignItems: 'center',
            }}
          >
            {typingUserName}님이 입력 중
            <TypingDots />
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
