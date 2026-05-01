DO $$
DECLARE
chat_id BIGINT;
    link_id BIGINT;
    i INT;
    j INT;
BEGIN
FOR i IN 1..1000 LOOP
        chat_id := 900000 + i;

INSERT INTO chats (id) VALUES (chat_id) ON CONFLICT DO NOTHING;

FOR j IN 1..100 LOOP
            INSERT INTO links (url, last_checked, last_updated)
            VALUES (
                'https://github.com/loadtest/repo-' || i || '-' || j,
                NOW(),
                NOW()
            )
            ON CONFLICT (url) DO NOTHING
            RETURNING id INTO link_id;

            IF link_id IS NOT NULL THEN
                INSERT INTO link_chat (link_id, chat_id) VALUES (link_id, chat_id)
                ON CONFLICT DO NOTHING;
END IF;
END LOOP;
END LOOP;
END $$;
