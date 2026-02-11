import { ListItemButton, ListItemAvatar, ListItemText, Typography, Avatar, Box } from '@mui/material';
import { Group as GroupIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { Room } from '../../types/room';

interface RoomListItemProps {
  room: Room;
}

export default function RoomListItem({ room }: RoomListItemProps) {
  const navigate = useNavigate();

  return (
    <ListItemButton
      onClick={() => navigate(`/room/${room.roomId || room.id}`)}
      sx={{ px: 2, py: 1.2 }}
    >
      <ListItemAvatar>
        <Avatar sx={{ bgcolor: '#ABC1D1', width: 48, height: 48 }}>
          <GroupIcon />
        </Avatar>
      </ListItemAvatar>
      <ListItemText
        primary={
          <Typography variant="body1" fontWeight={500} noWrap>
            {room.roomName}
          </Typography>
        }
        secondary={
          <Box display="flex" alignItems="center" gap={0.5}>
            <Typography variant="body2" color="text.secondary">
              {room.userCount || 0}명 참여중
            </Typography>
            {room.description && (
              <Typography variant="body2" color="text.secondary" noWrap>
                · {room.description}
              </Typography>
            )}
          </Box>
        }
      />
    </ListItemButton>
  );
}
