import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import AuthLayout from './layouts/AuthLayout';
import MainLayout from './layouts/MainLayout';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import FriendsPage from './pages/friends/FriendsPage';
import FriendSearchPage from './pages/friends/FriendSearchPage';
import ChatListPage from './pages/chat/ChatListPage';
import DMChatPage from './pages/chat/DMChatPage';
import RoomChatPage from './pages/chat/RoomChatPage';
import CreateRoomPage from './pages/rooms/CreateRoomPage';
import SearchPage from './pages/search/SearchPage';
import SettingsPage from './pages/settings/SettingsPage';
import LoadingSpinner from './components/common/LoadingSpinner';

export default function App() {
  const { isLoading } = useAuth();

  if (isLoading) {
    return <LoadingSpinner message="로딩 중..." />;
  }

  return (
    <Routes>
      {/* Auth routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      {/* Main routes with bottom tab bar */}
      <Route
        element={
          <ProtectedRoute>
            <MainLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/friends" element={<FriendsPage />} />
        <Route path="/friends/search" element={<FriendSearchPage />} />
        <Route path="/chats" element={<ChatListPage />} />
        <Route path="/rooms/create" element={<CreateRoomPage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>

      {/* Chat routes (full screen, no bottom tab) */}
      <Route
        path="/dm/:conversationId"
        element={
          <ProtectedRoute>
            <DMChatPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/room/:roomId"
        element={
          <ProtectedRoute>
            <RoomChatPage />
          </ProtectedRoute>
        }
      />

      {/* Default redirect */}
      <Route path="/" element={<Navigate to="/friends" replace />} />
      <Route path="*" element={<Navigate to="/friends" replace />} />
    </Routes>
  );
}
