package com.debankar.featureflags.domain;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Flag {

    private UUID id;
    private String key;
    private String name;
    private boolean enabled;
    private int rolloutPercentage;
    private List<TargetingRule> targetingRules;
    private List<Variant> variants;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Flag() {}

    public Flag(UUID id,
                String key,
                String name,
                boolean enabled,
                int rolloutPercentage,
                List<TargetingRule> targetingRules,
                List<Variant> variants,
                OffsetDateTime createdAt,
                OffsetDateTime updatedAt) {
        this.id                = id;
        this.key               = key;
        this.name              = name;
        this.enabled           = enabled;
        this.rolloutPercentage = rolloutPercentage;
        this.targetingRules    = targetingRules != null
                ? Collections.unmodifiableList(targetingRules)
                : Collections.emptyList();
        this.variants          = variants != null
                ? Collections.unmodifiableList(variants)
                : Collections.emptyList();
        this.createdAt         = createdAt;
        this.updatedAt         = updatedAt;
    }

    public boolean hasVariants() {
        return variants != null && !variants.isEmpty();
    }

    public boolean hasTargetingRules() {
        return targetingRules != null && !targetingRules.isEmpty();
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

    public List<TargetingRule> getTargetingRules() {
        return targetingRules;
    }
    public void setTargetingRules(List<TargetingRule> targetingRules) {
        this.targetingRules = targetingRules;
    }

    public List<Variant> getVariants() { return variants; }
    public void setVariants(List<Variant> variants) {
        this.variants = variants;
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Flag{" +
               "key='" + key + '\'' +
               ", enabled=" + enabled +
               ", rolloutPercentage=" + rolloutPercentage +
               '}';
    }
}