package com.debankar.featureflags.api.dto;

public class EvaluateResponseDto {

    private boolean enabled;
    private String  variant;
    private String  reason;

    public EvaluateResponseDto(boolean enabled,
                                String variant,
                                String reason) {
        this.enabled = enabled;
        this.variant = variant;
        this.reason  = reason;
    }

    public boolean isEnabled() { return enabled; }
    public String  getVariant() { return variant; }
    public String  getReason()  { return reason; }
}