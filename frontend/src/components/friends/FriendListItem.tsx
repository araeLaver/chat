import { ListItem, ListItemAvatar, ListItemText, IconButton, Typography, Box } from '@mui/material';
import { Chat as ChatIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import AvatarWithStatus from '../common/AvatarWithStatus';
import { Friend } from '../../types/friend';
import { dmApi } from '../../api/dm';

interface FriendListItemProps {
  friend: Friend;
  onUnfriend?: (id: number) => void;
}

export default function FriendListItem({ friend, onUnfriend }: FriendListItemProps) {
  const navigate = useNavigate();

  const handleChat = async () => {
    try {
      const res = await dmApi.startConversation(friend.userId);
      const conversationId = res.data.conversationId || res.data;
      navigate(`/dm/${conversationId}`);
    } catch {
      // fallback
    }
  };

  return (
    <ListItem
      sx={{ px: 2, py: 1, '&:hover': { bgcolor: 'action.hover' } }}
      secondaryAction={
        <Box>
          <IconButton size="small" onClick={handleChat}>
            <ChatIcon fontSize="small" />
          </IconButton>
          {onUnfriend && (
            <IconButton size="small" onClick={() => onUnfriend(friend.userId)} color="error">
              <DeleteIcon fontSize="small" />
            </IconButton>
          )}
        </Box>
      }
    >
      <ListItemAvatar>
        <AvatarWithStatus
          name={friend.displayName || friend.username}
          online={friend.online}
        />
      </ListItemAvatar>
      <ListItemText
        primary={
          <Typography variant="body1" fontWeight={500}>
            {friend.displayName || friend.username}
          </Typography>
        }
        secondary={friend.statusMessage || ''}
      />
    </ListItem>
  );
}
