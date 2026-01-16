import { Avatar as MuiAvatar, Badge, styled } from '@mui/material';

interface AvatarProps {
  src?: string;
  name?: string;
  size?: 'small' | 'medium' | 'large';
  isOnline?: boolean;
  showStatus?: boolean;
}

const sizeMap = {
  small: 32,
  medium: 40,
  large: 56,
};

const StyledBadge = styled(Badge, {
  shouldForwardProp: (prop) => prop !== 'isOnline',
})<{ isOnline?: boolean }>(({ theme, isOnline }) => ({
  '& .MuiBadge-badge': {
    backgroundColor: isOnline ? '#44b700' : '#bdbdbd',
    color: isOnline ? '#44b700' : '#bdbdbd',
    boxShadow: `0 0 0 2px ${theme.palette.background.paper}`,
    '&::after': {
      position: 'absolute',
      top: 0,
      left: 0,
      width: '100%',
      height: '100%',
      borderRadius: '50%',
      animation: isOnline ? 'ripple 1.2s infinite ease-in-out' : 'none',
      border: '1px solid currentColor',
      content: '""',
    },
  },
  '@keyframes ripple': {
    '0%': {
      transform: 'scale(.8)',
      opacity: 1,
    },
    '100%': {
      transform: 'scale(2.4)',
      opacity: 0,
    },
  },
}));

function getInitials(name?: string): string {
  if (!name) return '?';

  const parts = name.trim().split(' ');
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
}

function stringToColor(string: string): string {
  let hash = 0;
  for (let i = 0; i < string.length; i++) {
    hash = string.charCodeAt(i) + ((hash << 5) - hash);
  }

  const colors = [
    '#1976d2', '#388e3c', '#d32f2f', '#7b1fa2',
    '#c2185b', '#0288d1', '#689f38', '#ffa000',
    '#5d4037', '#455a64', '#00796b', '#512da8',
  ];

  return colors[Math.abs(hash) % colors.length];
}

export function Avatar({
  src,
  name,
  size = 'medium',
  isOnline,
  showStatus = false,
}: AvatarProps) {
  const pixelSize = sizeMap[size];

  const avatar = (
    <MuiAvatar
      src={src}
      sx={{
        width: pixelSize,
        height: pixelSize,
        bgcolor: src ? undefined : stringToColor(name || ''),
        fontSize: pixelSize * 0.4,
      }}
    >
      {!src && getInitials(name)}
    </MuiAvatar>
  );

  if (showStatus) {
    return (
      <StyledBadge
        overlap="circular"
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        variant="dot"
        isOnline={isOnline}
      >
        {avatar}
      </StyledBadge>
    );
  }

  return avatar;
}
