CREATE TABLE quiz_attempt
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id       VARCHAR(64)  NOT NULL,
    source_name     VARCHAR(200) NOT NULL,
    mode            VARCHAR(16)  NOT NULL,
    total_questions INT          NOT NULL,
    correct_answers INT          NOT NULL,
    duration_seconds INT,
    created_at      DATETIME     NOT NULL,
    INDEX idx_quiz_attempt_device_created (device_id, created_at)
);

CREATE TABLE quiz_attempt_answer
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id     BIGINT        NOT NULL,
    question_index INT           NOT NULL,
    question       VARCHAR(1000) NOT NULL,
    selected_text  VARCHAR(500),
    correct_text   VARCHAR(500),
    INDEX idx_quiz_attempt_answer_attempt (attempt_id)
);
