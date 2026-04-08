CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE flags (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    key                 VARCHAR(100) NOT NULL UNIQUE,
    name                VARCHAR(255) NOT NULL,
    enabled             BOOLEAN      NOT NULL DEFAULT FALSE,
    rollout_percentage  INTEGER      NOT NULL DEFAULT 0
                        CHECK (rollout_percentage BETWEEN 0 AND 100),
    targeting_rules     JSONB,
    variants            JSONB,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_flags_key        ON flags(key);
CREATE INDEX idx_flags_enabled    ON flags(enabled);
CREATE INDEX idx_flags_created_at ON flags(created_at);