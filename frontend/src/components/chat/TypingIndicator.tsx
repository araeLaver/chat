import { Box, Typography } from '@mui/material';

interface TypingIndicatorProps {
  username: string;
}

export default function TypingIndicator({ username }: TypingIndicatorProps) {
  return (
    <Box sx={{ px: 2, py: 0.5 }}>
      <Typography variant="caption" color="text.secondary" fontStyle="italic">
        {username}님이 입력 중...
      </Typography>
    </Box>
  );
}
