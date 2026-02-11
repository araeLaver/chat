export type RoomType = 'PUBLIC' | 'PRIVATE';

export interface Room {
  id: number;
  roomId: string;
  roomName: string;
  roomType: RoomType;
  description?: string;
  creator: string;
  userCount: number;
  maxMembers: number;
  isDirectMessage?: boolean;
  createdAt?: string;
}

export interface CreateRoomRequest {
  roomName: string;
  description?: string;
  roomType?: RoomType;
  maxMembers?: number;
}

export interface UpdateRoomRequest {
  roomName?: string;
  description?: string;
  maxMembers?: number;
}

export interface RoomMember {
  userId: number;
  username: string;
  displayName: string;
  role?: string;
}
