package com.debankar.featureflags.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class EvaluateRequest {

    @NotBlank(message = "Flag key is required")
    private String flagKey;

    @NotBlank(message = "User ID is required")
    private String userId;

    private Map<String, Object> attributes;

    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}