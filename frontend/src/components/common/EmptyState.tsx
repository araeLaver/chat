import { Box, Typography } from '@mui/material';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
}

export default function EmptyState({ icon, title, description }: EmptyStateProps) {
  return (
    <Box
      display="flex"
      flexDirection="column"
      justifyContent="center"
      alignItems="center"
      height="100%"
      minHeight={300}
      gap={1}
      px={3}
    >
      {icon && (
        <Box sx={{ mb: 1, color: 'text.secondary', '& .MuiSvgIcon-root': { fontSize: 48 } }}>
          {icon}
        </Box>
      )}
      <Typography variant="subtitle1" color="text.secondary" textAlign="center">
        {title}
      </Typography>
      {description && (
        <Typography variant="body2" color="text.secondary" textAlign="center">
          {description}
        </Typography>
      )}
    </Box>
  );
}
