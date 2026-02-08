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

  // File related
  fileName?: string;
  fileUrl?: string;
  fileSize?: number;
  mimeType?: string;

  // Security related
  securityType?: SecurityType;
  ttlSeconds?: number;
  expiresAt?: string;

  // Translation related
  sourceLanguage?: string;
  translatedContent?: string;
  translatedLanguage?: string;

  // Reply related
  replyToId?: number;
  replyToSender?: string;
  replyToContent?: string;

  // Reactions
  reactions?: MessageReaction[];

  // Read status
  isRead?: boolean;
  readCount?: number;

  // Mentions
  mentions?: string[];
}

export interface MessageReaction {
  id: number;
  emoji: string;
  userId: number;
  username: string;
  createdAt: string;
}

export interface TypingIndicator {
  roomId: string;
  userId: number;
  username: string;
  isTyping: boolean;
}
