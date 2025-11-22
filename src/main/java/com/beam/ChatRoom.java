package com.beam;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {
    private String roomId;
    private String roomName;
    private RoomType roomType = RoomType.GROUP;
    private String creator;
    private String description;
    private Map<String, User> users;
    private long createdTime;

    public ChatRoom() {
        this.users = new ConcurrentHashMap<>();
    }

    public ChatRoom(String roomId, String roomName) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.users = new ConcurrentHashMap<>();
        this.createdTime = System.currentTimeMillis();
    }
    
    public ChatRoom(String roomId, String roomName, RoomType roomType) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomType = roomType;
        this.users = new ConcurrentHashMap<>();
        this.createdTime = System.currentTimeMillis();
    }

    public ChatRoom(String roomId, String roomName, RoomType roomType, String creator, String description) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomType = roomType;
        this.creator = creator;
        this.description = description;
        this.users = new ConcurrentHashMap<>();
        this.createdTime = System.currentTimeMillis();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Map<String, User> getUsers() {
        return users;
    }

    public void setUsers(Map<String, User> users) {
        this.users = users;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public void addUser(User user) {
        users.put(user.getSessionId(), user);
    }

    public User removeUser(String sessionId) {
        return users.remove(sessionId);
    }

    public int getUserCount() {
        return users.size();
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public boolean isDirectMessage() {
        return roomType == RoomType.DIRECT;
    }

    public boolean isGroupChat() {
        return roomType == RoomType.GROUP;
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

}