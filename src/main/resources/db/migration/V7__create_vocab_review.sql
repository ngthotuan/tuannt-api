-- Per-user spaced-repetition state, one row per (user, word) once the word has been reviewed.
-- Words never reviewed have no row; the queue finds them with NOT EXISTS.
CREATE TABLE vocab_review
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT   NOT NULL,
    word_id          BIGINT   NOT NULL,
    ease             DOUBLE   NOT NULL,
    interval_days    INT      NOT NULL,
    repetitions      INT      NOT NULL,
    lapses           INT      NOT NULL,
    due_at           DATETIME NOT NULL,
    last_reviewed_at DATETIME NOT NULL,
    created_at       DATETIME NOT NULL,
    updated_at       DATETIME NOT NULL,
    UNIQUE KEY uk_vocab_review_user_word (user_id, word_id),
    INDEX idx_vocab_review_user_due (user_id, due_at),
    INDEX idx_vocab_review_word (word_id)
);

-- One row per answer. vocab_review only keeps the latest review, so the day streak and the
-- review count need this history. review_day is the UTC date, computed by the app, so the
-- streak never depends on the database session time zone.
CREATE TABLE vocab_review_log
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT     NOT NULL,
    word_id     BIGINT     NOT NULL,
    grade       VARCHAR(8) NOT NULL,
    reviewed_at DATETIME   NOT NULL,
    review_day  DATE       NOT NULL,
    INDEX idx_vocab_review_log_user_day (user_id, review_day),
    INDEX idx_vocab_review_log_word (word_id)
);
