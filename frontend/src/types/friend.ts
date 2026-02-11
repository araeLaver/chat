export type FriendshipStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'BLOCKED';

export interface Friend {
  id: number;
  userId: number;
  username: string;
  displayName: string;
  profileImageUrl?: string;
  statusMessage?: string;
  online?: boolean;
}

export interface FriendRequest {
  id: number;
  requesterId: number;
  requesterUsername: string;
  requesterDisplayName: string;
  status: FriendshipStatus;
  createdAt: string;
}

export interface UserSearchResult {
  id: number;
  username: string;
  displayName: string;
  profileImageUrl?: string;
  isFriend?: boolean;
  requestSent?: boolean;
}
