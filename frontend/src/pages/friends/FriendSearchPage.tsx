import { useState } from 'react';
import {
  Box,
  TextField,
  List,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  InputAdornment,
  Alert,
} from '@mui/material';
import { ArrowBack, Search } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { friendsApi } from '../../api/friends';
import { UserSearchResult as UserResult } from '../../types/friend';
import UserSearchResultItem from '../../components/friends/UserSearchResult';
import EmptyState from '../../components/common/EmptyState';

export default function FriendSearchPage() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<UserResult[]>([]);
  const [searched, setSearched] = useState(false);
  const [message, setMessage] = useState('');

  const handleSearch = async () => {
    if (!query.trim()) return;
    try {
      const res = await friendsApi.searchUsers(query.trim());
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

  const handleAddFriend = async (userId: number) => {
    try {
      await friendsApi.sendRequest(userId);
      setMessage('친구 요청을 보냈습니다.');
      setResults((prev) =>
        prev.map((u) => (u.id === userId ? { ...u, requestSent: true } : u))
      );
    } catch (err: any) {
      setMessage(err.response?.data?.message || err.response?.data || '요청에 실패했습니다.');
    }
  };

  return (
    <Box sx={{ height: '100%' }}>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <IconButton edge="start" onClick={() => navigate(-1)} sx={{ mr: 1 }}>
            <ArrowBack />
          </IconButton>
          <Typography variant="h6">친구 찾기</Typography>
        </Toolbar>
      </AppBar>

      <Box sx={{ p: 2 }}>
        <TextField
          fullWidth
          placeholder="아이디 또는 닉네임으로 검색"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
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
      </Box>

      {message && (
        <Box px={2}>
          <Alert severity="info" onClose={() => setMessage('')}>
            {message}
          </Alert>
        </Box>
      )}

      <List>
        {results.map((user) => (
          <UserSearchResultItem
            key={user.id}
            user={user}
            onAddFriend={handleAddFriend}
          />
        ))}
      </List>

      {searched && results.length === 0 && (
        <EmptyState title="검색 결과가 없습니다" description="다른 검색어로 시도해보세요" />
      )}
    </Box>
  );
}
