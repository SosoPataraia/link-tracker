-- liquibase formatted SQL
-- changeset scrapper:01-outbox-schema

CREATE TABLE outbox_events
(
    id           BIGSERIAL PRIMARY KEY,
    topic        TEXT        NOT NULL,
    key          TEXT,
    payload      TEXT        NOT NULL,
    status       TEXT        NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pending ON outbox_events (created_at)
    WHERE status = 'PENDING';
