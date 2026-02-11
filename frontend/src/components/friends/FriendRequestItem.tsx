import { ListItem, ListItemAvatar, ListItemText, Button, Stack, Typography } from '@mui/material';
import AvatarWithStatus from '../common/AvatarWithStatus';
import { FriendRequest } from '../../types/friend';

interface FriendRequestItemProps {
  request: FriendRequest;
  onAccept: (requesterId: number) => void;
  onReject: (requesterId: number) => void;
}

export default function FriendRequestItem({ request, onAccept, onReject }: FriendRequestItemProps) {
  return (
    <ListItem sx={{ px: 2, py: 1 }}>
      <ListItemAvatar>
        <AvatarWithStatus name={request.requesterDisplayName || request.requesterUsername} />
      </ListItemAvatar>
      <ListItemText
        primary={
          <Typography variant="body1" fontWeight={500}>
            {request.requesterDisplayName || request.requesterUsername}
          </Typography>
        }
        secondary="친구 요청"
      />
      <Stack direction="row" spacing={1}>
        <Button
          size="small"
          variant="contained"
          color="primary"
          onClick={() => onAccept(request.requesterId)}
          sx={{ minWidth: 56 }}
        >
          수락
        </Button>
        <Button
          size="small"
          variant="outlined"
          color="inherit"
          onClick={() => onReject(request.requesterId)}
          sx={{ minWidth: 56 }}
        >
          거절
        </Button>
      </Stack>
    </ListItem>
  );
}
