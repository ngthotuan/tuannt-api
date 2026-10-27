-- tags and tech are JSON arrays of strings kept in plain VARCHAR columns: no JSON column type,
-- so the same DDL runs on MySQL 8 and TiDB, and tag filtering is a LIKE on '"tag"'.
CREATE TABLE post
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug         VARCHAR(120) NOT NULL,
    title        VARCHAR(200) NOT NULL,
    excerpt      VARCHAR(500),
    content      MEDIUMTEXT   NOT NULL,
    cover_url    VARCHAR(500),
    tags         VARCHAR(500) NOT NULL,
    status       VARCHAR(16)  NOT NULL,
    published_at DATETIME,
    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME     NOT NULL,
    UNIQUE KEY uk_post_slug (slug),
    INDEX idx_post_status_published_at (status, published_at)
);

CREATE TABLE project
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug        VARCHAR(120)  NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    summary     VARCHAR(300)  NOT NULL,
    description MEDIUMTEXT,
    tech        VARCHAR(1500) NOT NULL,
    repo_url    VARCHAR(500),
    demo_url    VARCHAR(500),
    image_url   VARCHAR(500),
    featured    BOOLEAN       NOT NULL,
    sort_order  INT           NOT NULL,
    visible     BOOLEAN       NOT NULL,
    created_at  DATETIME      NOT NULL,
    updated_at  DATETIME      NOT NULL,
    UNIQUE KEY uk_project_slug (slug)
);
