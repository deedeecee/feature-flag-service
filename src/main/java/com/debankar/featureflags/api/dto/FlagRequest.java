package com.debankar.featureflags.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;

public class FlagRequest {

    @NotBlank(message = "Flag key is required")
    @Pattern(
            regexp = "^[a-z0-9][a-z0-9-]{1,98}[a-z0-9]$",
            message = "Key must be lowercase alphanumeric with hyphens, " +
                    "no leading or trailing hyphens"
    )
    private String key;

    @NotBlank(message = "Flag name is required")
    private String name;

    private boolean enabled = false;

    @Min(value = 0,   message = "Rollout percentage must be >= 0")
    @Max(value = 100, message = "Rollout percentage must be <= 100")
    private int rolloutPercentage = 0;

    private List<Map<String, Object>> targetingRules;
    private List<Map<String, Object>> variants;

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

    public List<Map<String, Object>> getTargetingRules() {
        return targetingRules;
    }
    public void setTargetingRules(List<Map<String, Object>> targetingRules) {
        this.targetingRules = targetingRules;
    }

    public List<Map<String, Object>> getVariants() { return variants; }
    public void setVariants(List<Map<String, Object>> variants) {
        this.variants = variants;
    }
}
