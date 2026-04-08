CREATE TABLE impressions (
    id           BIGSERIAL    PRIMARY KEY,
    flag_key     VARCHAR(100) NOT NULL,
    user_id      VARCHAR(255) NOT NULL,
    variant      VARCHAR(100),
    evaluated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_impressions_flag_key     ON impressions(flag_key);
CREATE INDEX idx_impressions_evaluated_at ON impressions(evaluated_at);
CREATE INDEX idx_impressions_flag_time    ON impressions(flag_key, evaluated_at);