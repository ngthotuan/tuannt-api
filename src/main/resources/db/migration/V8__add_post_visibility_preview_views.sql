-- PUBLIC: listed and readable by anyone. UNLISTED: readable by link, never listed (list, tags,
-- RSS, sitemap). MEMBERS: listed, but the body is only returned to logged-in users.
-- One ALTER per column: older TiDB versions reject multi-column schema changes.
ALTER TABLE post ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC';
-- Secret for /v1/posts/{slug}?preview=KEY, which shows drafts and scheduled posts; null = no link.
ALTER TABLE post ADD COLUMN preview_key VARCHAR(64) NULL;
ALTER TABLE post ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;
