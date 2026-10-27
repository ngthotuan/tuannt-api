-- Null means unread; existing messages start out unread.
ALTER TABLE contact_message ADD COLUMN read_at DATETIME NULL;
