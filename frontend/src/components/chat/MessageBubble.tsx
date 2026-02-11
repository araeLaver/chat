import { Box, Typography } from '@mui/material';
import AvatarWithStatus from '../common/AvatarWithStatus';
import { formatMessageTime } from '../../utils/dateFormat';
import { MY_BUBBLE_COLOR, OTHER_BUBBLE_COLOR } from '../../utils/constants';

interface MessageBubbleProps {
  content: string;
  timestamp: string;
  isMine: boolean;
  senderName?: string;
  showAvatar?: boolean;
  showName?: boolean;
  readCount?: number;
  isRead?: boolean;
  messageType?: string;
  fileUrl?: string;
  fileName?: string;
}

export default function MessageBubble({
  content,
  timestamp,
  isMine,
  senderName,
  showAvatar = true,
  showName = true,
  readCount,
  isRead,
  messageType,
  fileUrl,
  fileName,
}: MessageBubbleProps) {
  const isImage = messageType === 'IMAGE';
  const isFile = messageType === 'FILE';

  const renderContent = () => {
    if (isImage && fileUrl) {
      return (
        <Box
          component="img"
          src={fileUrl}
          alt="image"
          sx={{ maxWidth: 200, maxHeight: 200, borderRadius: 1, display: 'block' }}
        />
      );
    }
    if (isFile && fileName) {
      return (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="body2">📎 {fileName}</Typography>
        </Box>
      );
    }
    return (
      <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
        {content}
      </Typography>
    );
  };

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: isMine ? 'row-reverse' : 'row',
        alignItems: 'flex-end',
        gap: 1,
        px: 1.5,
        py: 0.3,
      }}
    >
      {/* Avatar */}
      {!isMine && showAvatar && senderName ? (
        <Box sx={{ mb: 0.5, flexShrink: 0 }}>
          <AvatarWithStatus name={senderName} size={36} />
        </Box>
      ) : !isMine ? (
        <Box sx={{ width: 36, flexShrink: 0 }} />
      ) : null}

      {/* Message column */}
      <Box sx={{ maxWidth: '70%', display: 'flex', flexDirection: 'column', alignItems: isMine ? 'flex-end' : 'flex-start' }}>
        {/* Sender name */}
        {!isMine && showName && senderName && (
          <Typography variant="caption" color="text.secondary" sx={{ mb: 0.3, ml: 0.5 }}>
            {senderName}
          </Typography>
        )}

        <Box sx={{ display: 'flex', flexDirection: isMine ? 'row-reverse' : 'row', alignItems: 'flex-end', gap: 0.5 }}>
          {/* Bubble */}
          <Box
            sx={{
              bgcolor: isMine ? MY_BUBBLE_COLOR : OTHER_BUBBLE_COLOR,
              px: 1.5,
              py: 1,
              borderRadius: 2,
              borderTopLeftRadius: !isMine ? 4 : undefined,
              borderTopRightRadius: isMine ? 4 : undefined,
              position: 'relative',
              boxShadow: '0 1px 2px rgba(0,0,0,0.06)',
            }}
          >
            {renderContent()}
          </Box>

          {/* Time + read status */}
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: isMine ? 'flex-end' : 'flex-start', gap: 0 }}>
            {isMine && readCount !== undefined && readCount > 0 && (
              <Typography variant="caption" sx={{ color: '#FEE500', fontSize: '0.65rem', lineHeight: 1 }}>
                {readCount}
              </Typography>
            )}
            {isMine && isRead !== undefined && (
              <Typography variant="caption" sx={{ color: isRead ? '#FEE500' : 'text.disabled', fontSize: '0.65rem', lineHeight: 1 }}>
                {isRead ? '읽음' : ''}
              </Typography>
            )}
            <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.65rem', lineHeight: 1.2 }}>
              {timestamp ? formatMessageTime(timestamp) : ''}
            </Typography>
          </Box>
        </Box>
      </Box>
    </Box>
  );
}
