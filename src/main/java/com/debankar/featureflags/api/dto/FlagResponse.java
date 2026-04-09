package com.debankar.featureflags.api.dto;

import com.debankar.featureflags.persistence.FlagEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlagResponse {

    private UUID   id;
    private String key;
    private String name;
    private boolean enabled;
    private int     rolloutPercentage;
    private List<Map<String, Object>> targetingRules;
    private List<Map<String, Object>> variants;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static FlagResponse from(FlagEntity entity) {
        FlagResponse r = new FlagResponse();
        r.id                = entity.getId();
        r.key               = entity.getKey();
        r.name              = entity.getName();
        r.enabled           = entity.isEnabled();
        r.rolloutPercentage = entity.getRolloutPercentage();
        r.targetingRules    = entity.getTargetingRules();
        r.variants          = entity.getVariants();
        r.createdAt         = entity.getCreatedAt();
        r.updatedAt         = entity.getUpdatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public String getKey() { return key; }
    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public int getRolloutPercentage() { return rolloutPercentage; }
    public List<Map<String, Object>> getTargetingRules() {
        return targetingRules;
    }
    public List<Map<String, Object>> getVariants() { return variants; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}