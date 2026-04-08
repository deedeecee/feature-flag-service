package com.debankar.featureflags.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "flags")
public class FlagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Column(name = "rollout_percentage", nullable = false)
    private int rolloutPercentage = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "targeting_rules", columnDefinition = "jsonb")
    private List<Map<String, Object>> targetingRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variants", columnDefinition = "jsonb")
    private List<Map<String, Object>> variants;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getRolloutPercentage() { return rolloutPercentage; }
    public void setRolloutPercentage(int rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }

    public List<Map<String, Object>> getTargetingRules() { return targetingRules; }
    public void setTargetingRules(List<Map<String, Object>> targetingRules) {
        this.targetingRules = targetingRules;
    }

    public List<Map<String, Object>> getVariants() { return variants; }
    public void setVariants(List<Map<String, Object>> variants) {
        this.variants = variants;
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
