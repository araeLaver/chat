import { useState } from 'react';
import {
  Box,
  Typography,
  List,
  Divider,
  IconButton,
  AppBar,
  Toolbar,
  Badge,
  Collapse,
} from '@mui/material';
import {
  PersonAdd,
  ExpandMore,
  ExpandLess,
  People as PeopleIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { useFriends } from '../../contexts/FriendContext';
import AvatarWithStatus from '../../components/common/AvatarWithStatus';
import FriendListItem from '../../components/friends/FriendListItem';
import FriendRequestItem from '../../components/friends/FriendRequestItem';
import EmptyState from '../../components/common/EmptyState';
import LoadingSpinner from '../../components/common/LoadingSpinner';

export default function FriendsPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const {
    friends,
    receivedRequests,
    requestCount,
    loading,
    acceptRequest,
    rejectRequest,
    unfriend,
  } = useFriends();
  const [showRequests, setShowRequests] = useState(true);

  if (loading && friends.length === 0) {
    return <LoadingSpinner message="친구 목록 불러오는 중..." />;
  }

  return (
    <Box sx={{ height: '100%' }}>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <Typography variant="h6" sx={{ flex: 1 }}>
            친구
          </Typography>
          <IconButton onClick={() => navigate('/friends/search')}>
            <Badge badgeContent={requestCount} color="error">
              <PersonAdd />
            </Badge>
          </IconButton>
        </Toolbar>
      </AppBar>

      <List disablePadding>
        {/* My profile */}
        {user && (
          <>
            <Box sx={{ px: 2, py: 1.5, display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <AvatarWithStatus name={user.displayName || user.username} size={52} />
              <Box>
                <Typography variant="subtitle1" fontWeight={600}>
                  {user.displayName || user.username}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  @{user.username}
                </Typography>
              </Box>
            </Box>
            <Divider />
          </>
        )}

        {/* Friend requests */}
        {receivedRequests.length > 0 && (
          <>
            <Box
              sx={{
                px: 2,
                py: 1,
                display: 'flex',
                alignItems: 'center',
                cursor: 'pointer',
              }}
              onClick={() => setShowRequests(!showRequests)}
            >
              <Typography variant="body2" color="text.secondary" sx={{ flex: 1 }}>
                친구 요청 {receivedRequests.length}
              </Typography>
              {showRequests ? <ExpandLess fontSize="small" /> : <ExpandMore fontSize="small" />}
            </Box>
            <Collapse in={showRequests}>
              {receivedRequests.map((req) => (
                <FriendRequestItem
                  key={req.id || req.requesterId}
                  request={req}
                  onAccept={acceptRequest}
                  onReject={rejectRequest}
                />
              ))}
            </Collapse>
            <Divider />
          </>
        )}

        {/* Friend count */}
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="body2" color="text.secondary">
            친구 {friends.length}
          </Typography>
        </Box>

        {/* Friend list */}
        {friends.length === 0 ? (
          <EmptyState
            icon={<PeopleIcon />}
            title="아직 친구가 없어요"
            description="사람 찾기에서 친구를 추가해보세요"
          />
        ) : (
          friends.map((friend) => (
            <FriendListItem
              key={friend.userId || friend.id}
              friend={friend}
              onUnfriend={unfriend}
            />
          ))
        )}
      </List>
    </Box>
  );
}
