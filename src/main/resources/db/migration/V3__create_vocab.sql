CREATE TABLE app_user
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    created_at    DATETIME     NOT NULL,
    UNIQUE KEY uk_app_user_username (username)
);

CREATE TABLE vocab_word
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    en         VARCHAR(255) NOT NULL,
    vi         VARCHAR(500) NOT NULL,
    created_at DATETIME     NOT NULL,
    UNIQUE KEY uk_vocab_word_en (en)
);

CREATE TABLE vocab_saved_word
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT   NOT NULL,
    word_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_vocab_saved_word_user_word (user_id, word_id),
    INDEX idx_vocab_saved_word_word (word_id)
);
