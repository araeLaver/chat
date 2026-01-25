package com.beam.exception;

/**
 * 채팅방 관련 예외
 */
public class RoomException extends ApplicationException {

    public RoomException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RoomException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public static RoomException notFound(Long roomId) {
        return new RoomException(ErrorCode.ROOM_NOT_FOUND, "Room ID: " + roomId);
    }

    public static RoomException accessDenied(Long roomId) {
        return new RoomException(ErrorCode.ROOM_ACCESS_DENIED, "Room ID: " + roomId);
    }

    public static RoomException roomFull(Long roomId) {
        return new RoomException(ErrorCode.ROOM_FULL, "Room ID: " + roomId);
    }

    public static RoomException alreadyMember(Long roomId, Long userId) {
        return new RoomException(ErrorCode.ROOM_ALREADY_MEMBER,
            "User " + userId + " is already a member of room " + roomId);
    }

    public static RoomException notMember(Long roomId, Long userId) {
        return new RoomException(ErrorCode.ROOM_NOT_MEMBER,
            "User " + userId + " is not a member of room " + roomId);
    }

    public static RoomException ownerOnly(Long roomId) {
        return new RoomException(ErrorCode.ROOM_OWNER_ONLY, "Room ID: " + roomId);
    }

    public static RoomException permissionDenied(Long roomId) {
        return new RoomException(ErrorCode.ROOM_PERMISSION_DENIED, "Room ID: " + roomId);
    }

    public static RoomException cannotRemoveOwner(Long roomId) {
        return new RoomException(ErrorCode.ROOM_CANNOT_REMOVE_OWNER, "Room ID: " + roomId);
    }

    public static RoomException userMuted(Long roomId, Long userId) {
        return new RoomException(ErrorCode.ROOM_USER_MUTED,
            "User " + userId + " is muted in room " + roomId);
    }
}
