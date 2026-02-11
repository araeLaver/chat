import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { Friend, FriendRequest } from '../types/friend';
import { friendsApi } from '../api/friends';
import { useAuth } from './AuthContext';

interface FriendContextType {
  friends: Friend[];
  receivedRequests: FriendRequest[];
  requestCount: number;
  loading: boolean;
  fetchFriends: () => Promise<void>;
  fetchRequests: () => Promise<void>;
  sendRequest: (friendId: number) => Promise<void>;
  acceptRequest: (requesterId: number) => Promise<void>;
  rejectRequest: (requesterId: number) => Promise<void>;
  unfriend: (friendId: number) => Promise<void>;
}

const FriendContext = createContext<FriendContextType | null>(null);

export function FriendProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  const [friends, setFriends] = useState<Friend[]>([]);
  const [receivedRequests, setReceivedRequests] = useState<FriendRequest[]>([]);
  const [requestCount, setRequestCount] = useState(0);
  const [loading, setLoading] = useState(false);

  const fetchFriends = useCallback(async () => {
    try {
      setLoading(true);
      const res = await friendsApi.getFriends();
      setFriends(Array.isArray(res.data) ? res.data : []);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchRequests = useCallback(async () => {
    try {
      const [reqRes, countRes] = await Promise.all([
        friendsApi.getReceivedRequests(),
        friendsApi.getRequestCount(),
      ]);
      setReceivedRequests(Array.isArray(reqRes.data) ? reqRes.data : []);
      setRequestCount(typeof countRes.data === 'number' ? countRes.data : 0);
    } catch {
      // ignore
    }
  }, []);

  const sendRequest = useCallback(async (friendId: number) => {
    await friendsApi.sendRequest(friendId);
  }, []);

  const acceptRequest = useCallback(
    async (requesterId: number) => {
      await friendsApi.acceptRequest(requesterId);
      await Promise.all([fetchFriends(), fetchRequests()]);
    },
    [fetchFriends, fetchRequests]
  );

  const rejectRequest = useCallback(
    async (requesterId: number) => {
      await friendsApi.rejectRequest(requesterId);
      await fetchRequests();
    },
    [fetchRequests]
  );

  const unfriend = useCallback(
    async (friendId: number) => {
      await friendsApi.unfriend(friendId);
      await fetchFriends();
    },
    [fetchFriends]
  );

  useEffect(() => {
    if (isAuthenticated) {
      fetchFriends();
      fetchRequests();
    }
  }, [isAuthenticated, fetchFriends, fetchRequests]);

  return (
    <FriendContext.Provider
      value={{
        friends,
        receivedRequests,
        requestCount,
        loading,
        fetchFriends,
        fetchRequests,
        sendRequest,
        acceptRequest,
        rejectRequest,
        unfriend,
      }}
    >
      {children}
    </FriendContext.Provider>
  );
}

export function useFriends() {
  const ctx = useContext(FriendContext);
  if (!ctx) throw new Error('useFriends must be used within FriendProvider');
  return ctx;
}
