package com.beam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * WebSocket 채팅 메시지 DTO
 * 유효성 검증 어노테이션 포함
 */
public class ChatMessage {

    @Size(max = 50, message = "발신자 이름은 50자를 초과할 수 없습니다")
    private String sender;

    @Size(max = 5000, message = "메시지 내용은 5000자를 초과할 수 없습니다")
    private String content;

    private String timestamp;

    @Pattern(regexp = "^(message|joinRoom|createRoom|createDirectMessage|deleteRoom|file|getHistory|markAsRead|ping|pong)?$",
             message = "유효하지 않은 메시지 타입입니다")
    private String type;

    @Size(max = 100, message = "방 ID는 100자를 초과할 수 없습니다")
    private String roomId;

    private MessageSecurityType securityType = MessageSecurityType.NORMAL;

    // 사용자 ID (1:1 채팅 및 친구 기능용)
    @Positive(message = "사용자 ID는 양수여야 합니다")
    private Long userId;

    @Positive(message = "친구 ID는 양수여야 합니다")
    private Long friendId;

    @Size(max = 50, message = "친구 이름은 50자를 초과할 수 없습니다")
    private String friendName;

    // 방 생성용 필드들
    @Size(max = 100, message = "방 이름은 100자를 초과할 수 없습니다")
    private String roomName;

    @Pattern(regexp = "^(public|private|direct)?$", message = "유효하지 않은 방 타입입니다")
    private String roomType;

    @Size(max = 50, message = "생성자 이름은 50자를 초과할 수 없습니다")
    private String creator;

    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;

    // TTL (Time-To-Live) for self-destructing messages
    private Long ttlSeconds;

    // Translation fields
    private String originalContent;
    private String sourceLanguage;
    private Boolean isTranslated = false;

    // Reply/Quote fields
    @Positive(message = "인용 메시지 ID는 양수여야 합니다")
    private Long replyToId;

    @Size(max = 50, message = "인용 발신자 이름은 50자를 초과할 수 없습니다")
    private String replyToSender;

    @Size(max = 200, message = "인용 내용은 200자를 초과할 수 없습니다")
    private String replyToContent;

    public ChatMessage() {}

    public ChatMessage(String sender, String content, String timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.type = "message";
    }

    public ChatMessage(String sender, String content, String timestamp, String type) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public MessageSecurityType getSecurityType() {
        return securityType;
    }

    public void setSecurityType(MessageSecurityType securityType) {
        this.securityType = securityType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFriendId() {
        return friendId;
    }

    public void setFriendId(Long friendId) {
        this.friendId = friendId;
    }

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    // 방 생성용 getter/setter
    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public Boolean getIsTranslated() {
        return isTranslated;
    }

    public void setIsTranslated(Boolean isTranslated) {
        this.isTranslated = isTranslated;
    }

    public Long getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(Long replyToId) {
        this.replyToId = replyToId;
    }

    public String getReplyToSender() {
        return replyToSender;
    }

    public void setReplyToSender(String replyToSender) {
        this.replyToSender = replyToSender;
    }

    public String getReplyToContent() {
        return replyToContent;
    }

    public void setReplyToContent(String replyToContent) {
        this.replyToContent = replyToContent;
    }
}