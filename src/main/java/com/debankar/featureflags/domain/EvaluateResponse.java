package com.debankar.featureflags.domain;

public class EvaluateResponse {

    public enum Reason {
        DISABLED,
        TARGETED,
        ROLLOUT,
        DEFAULT
    }

    private final boolean enabled;
    private final String variant;
    private final Reason reason;

    public EvaluateResponse(boolean enabled, String variant, Reason reason) {
        this.enabled = enabled;
        this.variant = variant;
        this.reason = reason;
    }

    public static EvaluateResponse disabled(Reason reason) {
        return new EvaluateResponse(false, null, reason);
    }

    public static EvaluateResponse enabled(String variant, Reason reason) {
        return new EvaluateResponse(true, variant, reason);
    }

    public boolean isEnabled() { return enabled; }
    public String getVariant() { return variant; }
    public Reason getReason()  { return reason; }

    @Override
    public String toString() {
        return "EvaluateResponse{" +
                "enabled=" + enabled +
                ", variant='" + variant + '\'' +
                ", reason=" + reason +
                '}';
    }
}
