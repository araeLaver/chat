package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emoticons", indexes = {
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_korean_name", columnList = "koreanName")
})
public class EmoticonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String koreanName;

    @Column(nullable = false, length = 10)
    private String emoji;

    @Column(length = 50)
    private String category;

    @Column(length = 200)
    private String keywords;

    @Column(nullable = false)
    private Boolean isPopular = false;

    @Column(nullable = false)
    private Integer usageCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public EmoticonEntity() {
    }

    public EmoticonEntity(Long id, String name, String koreanName, String emoji, String category,
                          String keywords, Boolean isPopular, Integer usageCount, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.koreanName = koreanName;
        this.emoji = emoji;
        this.category = category;
        this.keywords = keywords;
        this.isPopular = isPopular;
        this.usageCount = usageCount;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isPopular == null) {
            this.isPopular = false;
        }
        if (this.usageCount == null) {
            this.usageCount = 0;
        }
    }

    public void incrementUsage() {
        this.usageCount++;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getCategory() {
        return category;
    }

    public String getKeywords() {
        return keywords;
    }

    public Boolean getIsPopular() {
        return isPopular;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setKoreanName(String koreanName) {
        this.koreanName = koreanName;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public void setIsPopular(Boolean isPopular) {
        this.isPopular = isPopular;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String koreanName;
        private String emoji;
        private String category;
        private String keywords;
        private Boolean isPopular = false;
        private Integer usageCount = 0;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder koreanName(String koreanName) {
            this.koreanName = koreanName;
            return this;
        }

        public Builder emoji(String emoji) {
            this.emoji = emoji;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder keywords(String keywords) {
            this.keywords = keywords;
            return this;
        }

        public Builder isPopular(Boolean isPopular) {
            this.isPopular = isPopular;
            return this;
        }

        public Builder usageCount(Integer usageCount) {
            this.usageCount = usageCount;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public EmoticonEntity build() {
            EmoticonEntity entity = new EmoticonEntity();
            entity.id = this.id;
            entity.name = this.name;
            entity.koreanName = this.koreanName;
            entity.emoji = this.emoji;
            entity.category = this.category;
            entity.keywords = this.keywords;
            entity.isPopular = this.isPopular;
            entity.usageCount = this.usageCount;
            entity.createdAt = this.createdAt;
            return entity;
        }
    }
}
