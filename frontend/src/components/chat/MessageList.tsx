import { useEffect, useRef } from 'react';
import { Box, Typography } from '@mui/material';
import MessageBubble from './MessageBubble';
import { DirectMessage, RoomMessage } from '../../types/chat';
import { formatDateDivider } from '../../utils/dateFormat';
import { CHAT_BG_COLOR } from '../../utils/constants';

interface MessageListProps {
  messages: (DirectMessage | RoomMessage)[];
  currentUserId: number;
}

function isDM(msg: DirectMessage | RoomMessage): msg is DirectMessage {
  return 'conversationId' in msg;
}

function getDateKey(timestamp: string) {
  return timestamp ? timestamp.substring(0, 10) : '';
}

export default function MessageList({ messages, currentUserId }: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length]);

  let lastDate = '';
  let lastSenderId: number | null = null;

  return (
    <Box
      ref={containerRef}
      sx={{
        flex: 1,
        overflow: 'auto',
        bgcolor: CHAT_BG_COLOR,
        py: 1,
      }}
    >
      {messages.map((msg, idx) => {
        const timestamp = isDM(msg) ? msg.timestamp : msg.timestamp;
        const senderId = isDM(msg) ? msg.senderId : msg.senderId;
        const isMine = msg.isMine ?? senderId === currentUserId;
        const senderName = isDM(msg) ? undefined : (msg as RoomMessage).senderName;
        const dateKey = getDateKey(timestamp);
        const showDate = dateKey !== lastDate;
        const showAvatar = senderId !== lastSenderId || showDate;

        if (showDate) lastDate = dateKey;
        lastSenderId = senderId;

        return (
          <Box key={isDM(msg) ? msg.messageId : msg.id ?? idx}>
            {showDate && timestamp && (
              <Box sx={{ textAlign: 'center', my: 1.5 }}>
                <Typography
                  variant="caption"
                  sx={{
                    bgcolor: 'rgba(0,0,0,0.15)',
                    color: '#fff',
                    px: 2,
                    py: 0.5,
                    borderRadius: 10,
                    fontSize: '0.7rem',
                  }}
                >
                  {formatDateDivider(timestamp)}
                </Typography>
              </Box>
            )}
            <MessageBubble
              content={isDM(msg) ? msg.content : msg.content}
              timestamp={timestamp}
              isMine={isMine}
              senderName={senderName}
              showAvatar={showAvatar && !isMine}
              showName={showAvatar && !isMine}
              readCount={isDM(msg) ? undefined : (msg as RoomMessage).readCount}
              isRead={isDM(msg) ? msg.isRead : undefined}
              messageType={isDM(msg) ? msg.messageType : (msg as RoomMessage).messageType}
              fileUrl={isDM(msg) ? msg.fileUrl : undefined}
              fileName={isDM(msg) ? msg.fileName : undefined}
            />
          </Box>
        );
      })}
      <div ref={bottomRef} />
    </Box>
  );
}
