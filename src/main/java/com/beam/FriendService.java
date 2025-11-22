package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Friend Service
 *
 * <p>Manages friend relationships with caching for improved performance.
 * Cache eviction occurs automatically on friend status changes.
 *
 * @since 1.0.0
 */
@Service
public class FriendService {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "friends", key = "#userId"),
        @CacheEvict(value = "friends", key = "#friendId")
    })
    public FriendEntity sendFriendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new RuntimeException("Cannot add yourself as a friend");
        }

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        UserEntity friend = userRepository.findById(friendId)
            .orElseThrow(() -> new RuntimeException("Friend user not found"));

        Optional<FriendEntity> existingFriendship = friendRepository.findFriendship(userId, friendId);
        if (existingFriendship.isPresent()) {
            FriendEntity existing = existingFriendship.get();
            if (existing.getStatus() == FriendEntity.FriendStatus.BLOCKED) {
                throw new RuntimeException("Cannot send friend request to blocked user");
            }
            if (existing.getStatus() == FriendEntity.FriendStatus.PENDING) {
                throw new RuntimeException("Friend request already sent");
            }
            if (existing.getStatus() == FriendEntity.FriendStatus.ACCEPTED) {
                throw new RuntimeException("Already friends");
            }
        }

        FriendEntity friendRequest = FriendEntity.builder()
            .userId(userId)
            .friendId(friendId)
            .status(FriendEntity.FriendStatus.PENDING)
            .requestedAt(LocalDateTime.now())
            .build();

        return friendRepository.save(friendRequest);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "friends", key = "#userId"),
        @CacheEvict(value = "friends", key = "#requesterId")
    })
    public FriendEntity acceptFriendRequest(Long userId, Long requesterId) {
        FriendEntity friendRequest = friendRepository.findFriendship(requesterId, userId)
            .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!friendRequest.getFriendId().equals(userId)) {
            throw new RuntimeException("Cannot accept this friend request");
        }

        if (friendRequest.getStatus() != FriendEntity.FriendStatus.PENDING) {
            throw new RuntimeException("Friend request is not pending");
        }

        friendRequest.setStatus(FriendEntity.FriendStatus.ACCEPTED);
        friendRequest.setAcceptedAt(LocalDateTime.now());

        return friendRepository.save(friendRequest);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "friends", key = "#userId"),
        @CacheEvict(value = "friends", key = "#requesterId")
    })
    public void rejectFriendRequest(Long userId, Long requesterId) {
        FriendEntity friendRequest = friendRepository.findFriendship(requesterId, userId)
            .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!friendRequest.getFriendId().equals(userId)) {
            throw new RuntimeException("Cannot reject this friend request");
        }

        friendRequest.setStatus(FriendEntity.FriendStatus.REJECTED);
        friendRepository.save(friendRequest);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "friends", key = "#userId"),
        @CacheEvict(value = "friends", key = "#blockUserId")
    })
    public void blockUser(Long userId, Long blockUserId) {
        Optional<FriendEntity> existingFriendship = friendRepository.findFriendship(userId, blockUserId);

        if (existingFriendship.isPresent()) {
            FriendEntity friendship = existingFriendship.get();
            friendship.setStatus(FriendEntity.FriendStatus.BLOCKED);
            friendship.setBlockedAt(LocalDateTime.now());
            friendRepository.save(friendship);
        } else {
            FriendEntity blockRelation = FriendEntity.builder()
                .userId(userId)
                .friendId(blockUserId)
                .status(FriendEntity.FriendStatus.BLOCKED)
                .blockedAt(LocalDateTime.now())
                .build();
            friendRepository.save(blockRelation);
        }
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "friends", key = "#userId"),
        @CacheEvict(value = "friends", key = "#friendId")
    })
    public void unfriend(Long userId, Long friendId) {
        FriendEntity friendship = friendRepository.findFriendship(userId, friendId)
            .orElseThrow(() -> new RuntimeException("Friendship not found"));

        friendRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "friends", key = "#userId")
    public List<FriendEntity> getFriendList(Long userId) {
        return friendRepository.findAcceptedFriends(userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "friends", key = "'pending:' + #userId")
    public List<FriendEntity> getPendingRequestsReceived(Long userId) {
        return friendRepository.findPendingRequestsReceived(userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "friends", key = "'sent:' + #userId")
    public List<FriendEntity> getPendingRequestsSent(Long userId) {
        return friendRepository.findByUserIdAndStatus(userId, FriendEntity.FriendStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public Integer getPendingRequestCount(Long userId) {
        return friendRepository.countPendingRequests(userId);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> searchUsers(String query) {
        List<UserEntity> users = new ArrayList<>();

        Optional<UserEntity> byUsername = userRepository.findByUsername(query);
        byUsername.ifPresent(users::add);

        Optional<UserEntity> byPhone = userRepository.findByPhoneNumber(query);
        byPhone.ifPresent(user -> {
            if (!users.contains(user)) {
                users.add(user);
            }
        });

        return users;
    }
}
