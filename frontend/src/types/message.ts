export type MessageType =
  | 'message'
  | 'file'
  | 'image'
  | 'joinRoom'
  | 'leaveRoom'
  | 'createRoom'
  | 'getHistory'
  | 'markAsRead'
  | 'typing'
  | 'stopTyping'
  | 'reaction'
  | 'removeReaction'
  | 'bookmark'
  | 'removeBookmark'
  | 'ping'
  | 'pong'
  | 'dm'
  | 'dmHistory'
  | 'system';

export type SecurityType = 'NORMAL' | 'CONFIDENTIAL' | 'SECRET';

export interface ChatMessage {
  id?: number;
  type: MessageType;
  sender: string;
  content: string;
  timestamp: string;
  roomId?: string;
  userId?: number;

  // 파일 관련
  fileName?: string;
  fileUrl?: string;
  fileSize?: number;
  mimeType?: string;

  // 보안 관련
  securityType?: SecurityType;
  ttlSeconds?: number;
  expiresAt?: string;

  // 번역 관련
  sourceLanguage?: string;
  translatedContent?: string;
  translatedLanguage?: string;

  // 답장 관련
  replyToId?: number;
  replyToSender?: string;
  replyToContent?: string;

  // 리액션
  reactions?: MessageReaction[];

  // 읽음 상태
  isRead?: boolean;
  readCount?: number;

  // 멘션
  mentions?: string[];
}

export interface MessageReaction {
  id: number;
  emoji: string;
  userId: number;
  username: string;
  createdAt: string;
}

export interface MessageEntity {
  id: number;
  sender: string;
  content: string;
  roomId: string;
  messageType: string;
  timestamp: string;
  isEncrypted: boolean;
  securityType: SecurityType;
  sourceLanguage?: string;
  ttlSeconds?: number;
  expiresAt?: string;
  replyToId?: number;
  replyToSender?: string;
  replyToContent?: string;
  reactions?: MessageReaction[];
}

export interface DirectMessage {
  id: number;
  conversationId: string;
  senderId: number;
  receiverId: number;
  content: string;
  messageType: 'TEXT' | 'IMAGE' | 'FILE';
  timestamp: string;
  isRead: boolean;
  readAt?: string;
  isEncrypted: boolean;
  securityType: SecurityType;
  sourceLanguage?: string;
  ttlSeconds?: number;
  expiresAt?: string;
}

export interface Conversation {
  id: number;
  conversationId: string;
  user1Id: number;
  user2Id: number;
  lastMessage?: string;
  lastMessageTime?: string;
  lastMessageSenderId?: number;
  unreadCount1: number;
  unreadCount2: number;
  createdAt: string;
  otherUser?: {
    id: number;
    username: string;
    profileImage?: string;
    isOnline?: boolean;
  };
}

export interface TypingIndicator {
  roomId: string;
  userId: number;
  username: string;
  isTyping: boolean;
}
