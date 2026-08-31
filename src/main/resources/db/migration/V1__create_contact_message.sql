CREATE TABLE contact_message
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(255)  NOT NULL,
    subject    VARCHAR(200)  NOT NULL,
    message    VARCHAR(4000) NOT NULL,
    ip         VARCHAR(45),
    user_agent VARCHAR(500),
    created_at DATETIME      NOT NULL,
    INDEX idx_contact_message_created_at (created_at)
);
