export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'VOICE' | 'VIDEO' | 'LINK';

export interface DirectMessage {
  messageId: number;
  conversationId: string;
  senderId: number;
  receiverId: number;
  content: string;
  timestamp: string;
  isRead: boolean;
  messageType: MessageType;
  isMine: boolean;
  fileUrl?: string;
  fileName?: string;
  fileSize?: number;
}

export interface RoomMessage {
  id: number;
  senderId: number;
  senderName: string;
  content: string;
  messageType: MessageType;
  timestamp: string;
  readCount: number;
  isMine: boolean;
}

export interface Conversation {
  conversationId: string;
  otherUserId: number;
  otherUsername: string;
  otherDisplayName: string;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
}

export interface SendMessageRequest {
  content: string;
  messageType?: MessageType;
}

export interface WebSocketMessage {
  type: string;
  sender?: string;
  content?: string;
  roomId?: string;
  timestamp?: string;
  userId?: number;
  friendId?: number;
  friendName?: string;
  roomName?: string;
  creator?: string;
  description?: string;
  roomType?: string;
}
