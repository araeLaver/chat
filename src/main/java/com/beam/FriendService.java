package com.beam;

import com.beam.exception.FriendException;
import com.beam.exception.UserException;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "friends", key = "#userId"),
        @CacheEvict(value = "friends", key = "#friendId")
    })
    public FriendEntity sendFriendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw FriendException.selfRequest();
        }

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> UserException.notFound(userId));
        UserEntity friend = userRepository.findById(friendId)
            .orElseThrow(() -> UserException.notFound(friendId));

        Optional<FriendEntity> existingFriendship = friendRepository.findFriendship(userId, friendId);
        if (existingFriendship.isPresent()) {
            FriendEntity existing = existingFriendship.get();
            if (existing.getStatus() == FriendEntity.FriendStatus.BLOCKED) {
                throw FriendException.blockedUser(userId, friendId);
            }
            if (existing.getStatus() == FriendEntity.FriendStatus.PENDING) {
                throw FriendException.requestExists(userId, friendId);
            }
            if (existing.getStatus() == FriendEntity.FriendStatus.ACCEPTED) {
                throw FriendException.alreadyFriends(userId, friendId);
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
            .orElseThrow(() -> FriendException.requestNotFound(requesterId, userId));

        if (!friendRequest.getFriendId().equals(userId)) {
            throw FriendException.unauthorized(userId, friendRequest.getId());
        }

        if (friendRequest.getStatus() != FriendEntity.FriendStatus.PENDING) {
            throw FriendException.requestNotPending(requesterId, userId);
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
            .orElseThrow(() -> FriendException.requestNotFound(requesterId, userId));

        if (!friendRequest.getFriendId().equals(userId)) {
            throw FriendException.unauthorized(userId, friendRequest.getId());
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
            .orElseThrow(() -> FriendException.requestNotFound(userId, friendId));

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
