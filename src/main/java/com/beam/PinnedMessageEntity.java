package com.beam;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pinned_messages",
       uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "message_type", "context_type", "context_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedMessageEntity {

    public enum MessageType {
        ROOM, DIRECT, GROUP
    }

    public enum ContextType {
        ROOM, CONVERSATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 20)
    private ContextType contextType;

    @Column(name = "context_id", nullable = false, length = 100)
    private String contextId;

    @Column(name = "pinned_by_user_id", nullable = false)
    private Long pinnedByUserId;

    @Column(name = "pin_order")
    private Integer pinOrder;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
