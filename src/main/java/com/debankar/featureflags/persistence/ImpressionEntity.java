package com.debankar.featureflags.persistence;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "impressions")
public class ImpressionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_key", nullable = false, length = 100)
    private String flagKey;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "variant", length = 100)
    private String variant;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        evaluatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }

    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }

    public OffsetDateTime getEvaluatedAt() { return evaluatedAt; }
}
