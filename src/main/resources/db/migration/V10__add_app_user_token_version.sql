-- Bumped on password change, role change and deletion; tokens carry the version they were
-- issued with ("ver") and are rejected once it no longer matches. Tokens issued before this
-- column existed have no "ver" and count as version 0, so nobody is logged out by the deploy.
ALTER TABLE app_user ADD COLUMN token_version INT NOT NULL DEFAULT 0;
