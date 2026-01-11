package com.beam.exception;

/**
 * 친구 관련 예외
 */
public class FriendException extends ApplicationException {

    public FriendException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FriendException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public static FriendException requestExists(Long userId, Long friendId) {
        return new FriendException(ErrorCode.FRIEND_REQUEST_EXISTS,
            "From user " + userId + " to user " + friendId);
    }

    public static FriendException requestNotFound(Long userId, Long friendId) {
        return new FriendException(ErrorCode.FRIEND_REQUEST_NOT_FOUND,
            "From user " + userId + " to user " + friendId);
    }

    public static FriendException alreadyFriends(Long userId, Long friendId) {
        return new FriendException(ErrorCode.FRIEND_ALREADY_FRIENDS,
            "Users " + userId + " and " + friendId);
    }

    public static FriendException selfRequest() {
        return new FriendException(ErrorCode.FRIEND_SELF_REQUEST);
    }
}
