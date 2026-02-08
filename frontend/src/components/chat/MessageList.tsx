import { useEffect, useRef } from 'react';
import { Box, Typography, Divider } from '@mui/material';
import { MessageItem } from './MessageItem';
import type { ChatMessage } from '../../types';
import { format, isToday, isYesterday, isSameDay, differenceInMinutes } from 'date-fns';
import { ko } from 'date-fns/locale';

interface MessageListProps {
  messages: ChatMessage[];
  roomId: string;
}

interface MessageGroupInfo {
  showAvatar: boolean;
  showSenderName: boolean;
  showTime: boolean;
  isFirstInGroup: boolean;
  isLastInGroup: boolean;
}

function formatDateDivider(date: Date): string {
  if (isToday(date)) {
    return '오늘';
  }
  if (isYesterday(date)) {
    return '어제';
  }
  return format(date, 'yyyy년 M월 d일 (E)', { locale: ko });
}

function getMessageGroupInfo(
  messages: ChatMessage[],
  index: number
): MessageGroupInfo {
  const message = messages[index];
  const prevMessage = index > 0 ? messages[index - 1] : null;
  const nextMessage = index < messages.length - 1 ? messages[index + 1] : null;

  const messageDate = new Date(message.timestamp);
  const prevDate = prevMessage ? new Date(prevMessage.timestamp) : null;
  const nextDate = nextMessage ? new Date(nextMessage.timestamp) : null;

  // Check if same sender and within 1 minute
  const isSameSenderAsPrev =
    prevMessage &&
    prevMessage.sender === message.sender &&
    prevMessage.type !== 'system' &&
    message.type !== 'system' &&
    prevDate &&
    differenceInMinutes(messageDate, prevDate) < 1;

  const isSameSenderAsNext =
    nextMessage &&
    nextMessage.sender === message.sender &&
    nextMessage.type !== 'system' &&
    message.type !== 'system' &&
    nextDate &&
    differenceInMinutes(nextDate, messageDate) < 1;

  const isFirstInGroup = !isSameSenderAsPrev;
  const isLastInGroup = !isSameSenderAsNext;

  return {
    showAvatar: isFirstInGroup,
    showSenderName: isFirstInGroup,
    showTime: isLastInGroup,
    isFirstInGroup,
    isLastInGroup,
  };
}

export function MessageList({ messages }: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // 새 메시지가 오면 스크롤
  useEffect(() => {
    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages.length]);

  if (messages.length === 0) {
    return (
      <Box
        sx={{
          height: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Typography variant="body2" color="text.secondary">
          메시지가 없습니다. 첫 번째 메시지를 보내보세요!
        </Typography>
      </Box>
    );
  }

  // 날짜별 그룹화
  let lastDate: Date | null = null;

  return (
    <Box
      ref={containerRef}
      sx={{
        height: '100%',
        overflow: 'auto',
        p: 2,
        display: 'flex',
        flexDirection: 'column',
        gap: 0.5,
      }}
    >
      {messages.map((message, index) => {
        const messageDate = new Date(message.timestamp);
        const showDateDivider = !lastDate || !isSameDay(messageDate, lastDate);
        lastDate = messageDate;

        const groupInfo = getMessageGroupInfo(messages, index);

        return (
          <Box key={message.id || index}>
            {showDateDivider && (
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  my: 2,
                }}
              >
                <Divider sx={{ flex: 1 }} />
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ mx: 2 }}
                >
                  {formatDateDivider(messageDate)}
                </Typography>
                <Divider sx={{ flex: 1 }} />
              </Box>
            )}
            <MessageItem
              message={message}
              showAvatar={groupInfo.showAvatar}
              showSenderName={groupInfo.showSenderName}
              showTime={groupInfo.showTime}
              isFirstInGroup={groupInfo.isFirstInGroup}
              isLastInGroup={groupInfo.isLastInGroup}
            />
          </Box>
        );
      })}
      <div ref={bottomRef} />
    </Box>
  );
}
