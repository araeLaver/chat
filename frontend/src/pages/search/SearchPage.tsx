import { useState } from 'react';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  TextField,
  InputAdornment,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Chip,
  Stack,
} from '@mui/material';
import { Search } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { searchApi, SearchResult } from '../../api/search';
import { formatChatListTime } from '../../utils/dateFormat';
import EmptyState from '../../components/common/EmptyState';

export default function SearchPage() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [searched, setSearched] = useState(false);
  const [filter, setFilter] = useState<'ALL' | 'DM' | 'ROOM'>('ALL');

  const handleSearch = async () => {
    if (!keyword.trim()) return;
    try {
      const res = await searchApi.searchMessages(
        keyword.trim(),
        filter === 'ALL' ? undefined : filter
      );
      setResults(Array.isArray(res.data) ? res.data : []);
      setSearched(true);
    } catch {
      setResults([]);
      setSearched(true);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch();
  };

  const handleResultClick = (result: SearchResult) => {
    if (result.type === 'DM' && result.conversationId) {
      navigate(`/dm/${result.conversationId}`);
    } else if (result.type === 'ROOM' && result.roomId) {
      navigate(`/room/${result.roomId}`);
    }
  };

  return (
    <Box sx={{ height: '100%' }}>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <Typography variant="h6">검색</Typography>
        </Toolbar>
      </AppBar>

      <Box sx={{ p: 2 }}>
        <TextField
          fullWidth
          placeholder="메시지 검색"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={handleKeyDown}
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton onClick={handleSearch}>
                  <Search />
                </IconButton>
              </InputAdornment>
            ),
          }}
          autoFocus
        />

        <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
          {(['ALL', 'DM', 'ROOM'] as const).map((f) => (
            <Chip
              key={f}
              label={f === 'ALL' ? '전체' : f === 'DM' ? '1:1 채팅' : '그룹 채팅'}
              onClick={() => setFilter(f)}
              color={filter === f ? 'primary' : 'default'}
              variant={filter === f ? 'filled' : 'outlined'}
              size="small"
            />
          ))}
        </Stack>
      </Box>

      <List disablePadding>
        {results.map((result) => (
          <ListItemButton
            key={result.id}
            onClick={() => handleResultClick(result)}
            sx={{ px: 2, py: 1 }}
          >
            <ListItemText
              primary={
                <Box display="flex" justifyContent="space-between" alignItems="center">
                  <Typography variant="body2" fontWeight={500}>
                    {result.senderName}
                  </Typography>
                  <Chip
                    label={result.type === 'DM' ? '1:1' : '그룹'}
                    size="small"
                    variant="outlined"
                    sx={{ height: 20, fontSize: '0.7rem' }}
                  />
                </Box>
              }
              secondary={
                <Box>
                  <Typography variant="body2" color="text.primary" noWrap>
                    {result.content}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {result.timestamp ? formatChatListTime(result.timestamp) : ''}
                  </Typography>
                </Box>
              }
            />
          </ListItemButton>
        ))}
      </List>

      {searched && results.length === 0 && (
        <EmptyState title="검색 결과가 없습니다" description="다른 키워드로 검색해보세요" />
      )}
    </Box>
  );
}
