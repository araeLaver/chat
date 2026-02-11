import {
  Drawer,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Typography,
  Box,
  IconButton,
  Divider,
} from '@mui/material';
import { Close } from '@mui/icons-material';
import AvatarWithStatus from '../common/AvatarWithStatus';
import { RoomMember } from '../../types/room';

interface RoomMemberListProps {
  open: boolean;
  onClose: () => void;
  members: RoomMember[];
  roomName: string;
}

export default function RoomMemberList({ open, onClose, members, roomName }: RoomMemberListProps) {
  return (
    <Drawer anchor="right" open={open} onClose={onClose}>
      <Box sx={{ width: 280 }}>
        <Box sx={{ p: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="subtitle1" fontWeight={600}>
            {roomName}
          </Typography>
          <IconButton onClick={onClose} size="small">
            <Close />
          </IconButton>
        </Box>
        <Divider />
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="body2" color="text.secondary">
            멤버 {members.length}
          </Typography>
        </Box>
        <List disablePadding>
          {members.map((member) => (
            <ListItem key={member.userId} sx={{ px: 2, py: 0.8 }}>
              <ListItemAvatar>
                <AvatarWithStatus name={member.displayName || member.username} size={36} />
              </ListItemAvatar>
              <ListItemText
                primary={member.displayName || member.username}
                secondary={member.role === 'OWNER' ? '방장' : undefined}
              />
            </ListItem>
          ))}
        </List>
      </Box>
    </Drawer>
  );
}
