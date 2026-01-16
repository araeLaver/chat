export interface Friend {
  id: number;
  friendId: number;
  username: string;
  profileImage?: string;
  status: 'PENDING' | 'ACCEPTED' | 'BLOCKED';
  createdAt: string;
  isOnline?: boolean;
  lastSeenAt?: string;
}

export interface FriendRequest {
  id: number;
  senderId: number;
  senderUsername: string;
  senderProfileImage?: string;
  receiverId: number;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}

export interface UserSearchResult {
  id: number;
  username: string;
  profileImage?: string;
  isFriend: boolean;
  hasPendingRequest: boolean;
}

export interface UserSettings {
  ghostMode: boolean;
  disableIpLogging: boolean;
  autoTranslate: boolean;
  preferredLanguage: string;
  theme: 'light' | 'dark' | 'system';
  notificationsEnabled: boolean;
  soundEnabled: boolean;
}

export interface TranslationSettings {
  autoTranslate: boolean;
  preferredLanguage: string;
  serviceEnabled: boolean;
  supportedLanguages: string[];
}

export interface PrivacySettings {
  ghostMode: boolean;
  disableIpLogging: boolean;
  showOnlineStatus: boolean;
  showLastSeen: boolean;
}
