import { AppBar, Toolbar, Typography, IconButton, Box } from '@mui/material';
import { ArrowBack, MoreVert } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';

interface ChatHeaderProps {
  title: string;
  subtitle?: string;
  onBack?: () => void;
  onMenu?: () => void;
}

export default function ChatHeader({ title, subtitle, onBack, onMenu }: ChatHeaderProps) {
  const navigate = useNavigate();

  return (
    <AppBar position="static" elevation={0}>
      <Toolbar sx={{ minHeight: 56 }}>
        <IconButton edge="start" onClick={onBack || (() => navigate(-1))} sx={{ mr: 1 }}>
          <ArrowBack />
        </IconButton>
        <Box sx={{ flex: 1 }}>
          <Typography variant="subtitle1" fontWeight={600} noWrap>
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="caption" color="text.secondary">
              {subtitle}
            </Typography>
          )}
        </Box>
        {onMenu && (
          <IconButton edge="end" onClick={onMenu}>
            <MoreVert />
          </IconButton>
        )}
      </Toolbar>
    </AppBar>
  );
}
