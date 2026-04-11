-- initial schema placeholder
-- liquibase formatted SQL

CREATE TABLE chats
(
    id      BIGINT PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE links
(
    id           BIGSERIAL PRIMARY KEY,
    url          TEXT        NOT NULL UNIQUE,
    last_checked TIMESTAMPTZ,
    last_updated TIMESTAMPTZ
);

CREATE INDEX idx_links_url ON links (url);

CREATE TABLE link_chat
(
    link_id BIGINT NOT NULL REFERENCES links (id) ON DELETE CASCADE,
    chat_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    PRIMARY KEY (link_id, chat_id)
);

CREATE INDEX idx_link_chat_chat_id ON link_chat (chat_id);

CREATE TABLE link_tags
(
    id      BIGSERIAL PRIMARY KEY,
    link_id BIGINT NOT NULL REFERENCES links (id) ON DELETE CASCADE,
    chat_id BIGINT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    tag     TEXT   NOT NULL
);

CREATE INDEX idx_link_tags_link_chat ON link_tags (link_id, chat_id);
