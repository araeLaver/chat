package com.beam;

import com.beam.NotificationEntity.NotificationType;
import com.beam.PinnedMessageEntity.ContextType;
import com.beam.PinnedMessageEntity.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PinService {

    @Autowired
    private PinnedMessageRepository pinnedMessageRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private DirectMessageRepository directMessageRepository;

    @Autowired
    private NotificationService notificationService;

    @Value("${app.pin.max-per-context:50}")
    private int maxPinsPerContext;

    @Transactional
    public PinnedMessageEntity pinMessage(Long messageId, MessageType messageType,
                                           ContextType contextType, String contextId,
                                           Long userId, String note) {
        // Check if already pinned
        if (pinnedMessageRepository.existsByMessageIdAndMessageTypeAndContextTypeAndContextId(
                messageId, messageType, contextType, contextId)) {
            throw new IllegalStateException("Message is already pinned in this context");
        }

        // Check max pins limit
        Integer currentCount = pinnedMessageRepository.countByContext(contextType, contextId);
        if (currentCount >= maxPinsPerContext) {
            throw new IllegalStateException("Maximum number of pinned messages reached: " + maxPinsPerContext);
        }

        // Get next pin order
        Integer nextOrder = pinnedMessageRepository.getMaxPinOrder(contextType, contextId) + 1;

        PinnedMessageEntity pinnedMessage = PinnedMessageEntity.builder()
            .messageId(messageId)
            .messageType(messageType)
            .contextType(contextType)
            .contextId(contextId)
            .pinnedByUserId(userId)
            .pinOrder(nextOrder)
            .note(note)
            .build();

        PinnedMessageEntity saved = pinnedMessageRepository.save(pinnedMessage);

        // Create notification for message author
        createPinNotification(messageId, messageType, userId, contextId);

        return saved;
    }

    @Transactional
    public void unpinMessage(Long messageId, MessageType messageType,
                             ContextType contextType, String contextId) {
        pinnedMessageRepository.deleteByMessageIdAndMessageTypeAndContextTypeAndContextId(
            messageId, messageType, contextType, contextId);
    }

    @Transactional(readOnly = true)
    public List<PinnedMessageEntity> getPinnedMessages(ContextType contextType, String contextId) {
        return pinnedMessageRepository.findByContextTypeAndContextIdOrderByPinOrderAscCreatedAtDesc(
            contextType, contextId);
    }

    @Transactional(readOnly = true)
    public boolean isPinned(Long messageId, MessageType messageType,
                            ContextType contextType, String contextId) {
        return pinnedMessageRepository.existsByMessageIdAndMessageTypeAndContextTypeAndContextId(
            messageId, messageType, contextType, contextId);
    }

    @Transactional(readOnly = true)
    public Integer getPinnedCount(ContextType contextType, String contextId) {
        return pinnedMessageRepository.countByContext(contextType, contextId);
    }

    @Transactional
    public void updatePinOrder(Long pinnedMessageId, Integer newOrder, Long userId) {
        PinnedMessageEntity pinnedMessage = pinnedMessageRepository.findById(pinnedMessageId)
            .orElseThrow(() -> new IllegalArgumentException("Pinned message not found"));

        pinnedMessage.setPinOrder(newOrder);
        pinnedMessageRepository.save(pinnedMessage);
    }

    @Transactional
    public void updatePinNote(Long pinnedMessageId, String note, Long userId) {
        PinnedMessageEntity pinnedMessage = pinnedMessageRepository.findById(pinnedMessageId)
            .orElseThrow(() -> new IllegalArgumentException("Pinned message not found"));

        pinnedMessage.setNote(note);
        pinnedMessageRepository.save(pinnedMessage);
    }

    private void createPinNotification(Long messageId, MessageType messageType, Long actorUserId, String roomId) {
        Long messageAuthorId = null;

        if (messageType == MessageType.ROOM || messageType == MessageType.GROUP) {
            Optional<MessageEntity> messageOpt = messageRepository.findById(messageId);
            if (messageOpt.isPresent()) {
                MessageEntity message = messageOpt.get();
                // Get author ID from sender username
                // Note: In a real implementation, you might want to store userId directly
            }
        } else if (messageType == MessageType.DIRECT) {
            Optional<DirectMessageEntity> dmOpt = directMessageRepository.findById(messageId);
            if (dmOpt.isPresent()) {
                messageAuthorId = dmOpt.get().getSenderId();
            }
        }

        if (messageAuthorId != null && !messageAuthorId.equals(actorUserId)) {
            notificationService.createPinNotification(messageAuthorId, actorUserId, messageId, roomId);
        }
    }
}
