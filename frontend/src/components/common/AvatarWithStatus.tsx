import { Avatar, Badge, styled } from '@mui/material';

const OnlineBadge = styled(Badge)(({ theme }) => ({
  '& .MuiBadge-badge': {
    backgroundColor: '#44b700',
    color: '#44b700',
    boxShadow: `0 0 0 2px ${theme.palette.background.paper}`,
    width: 10,
    height: 10,
    borderRadius: '50%',
  },
}));

interface AvatarWithStatusProps {
  name: string;
  src?: string;
  online?: boolean;
  size?: number;
}

function stringToColor(string: string) {
  let hash = 0;
  for (let i = 0; i < string.length; i++) {
    hash = string.charCodeAt(i) + ((hash << 5) - hash);
  }
  const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F'];
  return colors[Math.abs(hash) % colors.length];
}

export default function AvatarWithStatus({ name, src, online, size = 40 }: AvatarWithStatusProps) {
  const avatar = (
    <Avatar
      src={src}
      sx={{ width: size, height: size, bgcolor: stringToColor(name), fontSize: size * 0.4 }}
    >
      {name.charAt(0).toUpperCase()}
    </Avatar>
  );

  if (online !== undefined) {
    return (
      <OnlineBadge
        overlap="circular"
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        variant={online ? 'dot' : 'standard'}
      >
        {avatar}
      </OnlineBadge>
    );
  }

  return avatar;
}
