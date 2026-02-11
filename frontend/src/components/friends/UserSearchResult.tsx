import { ListItem, ListItemAvatar, ListItemText, Button, Typography } from '@mui/material';
import { PersonAdd } from '@mui/icons-material';
import AvatarWithStatus from '../common/AvatarWithStatus';
import { UserSearchResult as UserResult } from '../../types/friend';

interface UserSearchResultProps {
  user: UserResult;
  onAddFriend: (userId: number) => void;
}

export default function UserSearchResultItem({ user, onAddFriend }: UserSearchResultProps) {
  return (
    <ListItem sx={{ px: 2, py: 1 }}>
      <ListItemAvatar>
        <AvatarWithStatus name={user.displayName || user.username} />
      </ListItemAvatar>
      <ListItemText
        primary={
          <Typography variant="body1" fontWeight={500}>
            {user.displayName || user.username}
          </Typography>
        }
        secondary={`@${user.username}`}
      />
      {user.isFriend ? (
        <Typography variant="body2" color="text.secondary">
          친구
        </Typography>
      ) : user.requestSent ? (
        <Typography variant="body2" color="text.secondary">
          요청됨
        </Typography>
      ) : (
        <Button
          size="small"
          variant="contained"
          color="primary"
          startIcon={<PersonAdd />}
          onClick={() => onAddFriend(user.id)}
        >
          추가
        </Button>
      )}
    </ListItem>
  );
}
