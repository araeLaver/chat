import { ListItemButton, ListItemAvatar, ListItemText, Typography, Badge, Box } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import AvatarWithStatus from '../common/AvatarWithStatus';
import { Conversation } from '../../types/chat';
import { formatChatListTime } from '../../utils/dateFormat';

interface ConversationListItemProps {
  conversation: Conversation;
}

export default function ConversationListItem({ conversation }: ConversationListItemProps) {
  const navigate = useNavigate();

  return (
    <ListItemButton
      onClick={() => navigate(`/dm/${conversation.conversationId}`)}
      sx={{ px: 2, py: 1.2 }}
    >
      <ListItemAvatar>
        <AvatarWithStatus
          name={conversation.otherDisplayName || conversation.otherUsername}
          size={48}
        />
      </ListItemAvatar>
      <ListItemText
        primary={
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="body1" fontWeight={conversation.unreadCount ? 700 : 400} noWrap>
              {conversation.otherDisplayName || conversation.otherUsername}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ ml: 1, flexShrink: 0 }}>
              {conversation.lastMessageTime ? formatChatListTime(conversation.lastMessageTime) : ''}
            </Typography>
          </Box>
        }
        secondary={
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography
              variant="body2"
              color="text.secondary"
              noWrap
              sx={{ flex: 1, fontWeight: conversation.unreadCount ? 600 : 400 }}
            >
              {conversation.lastMessage || ''}
            </Typography>
            {conversation.unreadCount > 0 && (
              <Badge
                badgeContent={conversation.unreadCount}
                color="error"
                max={999}
                sx={{ ml: 1 }}
              />
            )}
          </Box>
        }
      />
    </ListItemButton>
  );
}
