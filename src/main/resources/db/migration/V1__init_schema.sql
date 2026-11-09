-- SupportIQ relational schema (MySQL 8.4 LTS).
-- Flyway owns the schema; Hibernate runs with ddl-auto: none.

CREATE TABLE customers (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(160) NOT NULL,
    email      VARCHAR(190) NOT NULL,
    company    VARCHAR(120) NULL,
    tier       VARCHAR(20)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_customers_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE tickets (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    reference      VARCHAR(20)   NOT NULL,
    customer_id    BIGINT        NOT NULL,
    subject        VARCHAR(200)  NOT NULL,
    description    VARCHAR(4000) NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    priority       VARCHAR(20)   NOT NULL,
    assigned_agent VARCHAR(120)  NULL,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tickets_reference UNIQUE (reference),
    CONSTRAINT fk_tickets_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    INDEX idx_tickets_status_priority (status, priority),
    INDEX idx_tickets_customer (customer_id),
    INDEX idx_tickets_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE ticket_messages (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    ticket_id   BIGINT       NOT NULL,
    author_type VARCHAR(20)  NOT NULL,
    author_name VARCHAR(120) NOT NULL,
    body        VARCHAR(8000) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_messages_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    INDEX idx_messages_ticket_created (ticket_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE knowledge_documents (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    title          VARCHAR(200) NOT NULL,
    source         VARCHAR(300) NULL,
    content_hash   CHAR(64)     NOT NULL,
    content        LONGTEXT     NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    chunk_count    INT          NOT NULL DEFAULT 0,
    failure_reason VARCHAR(500) NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_knowledge_hash UNIQUE (content_hash),
    INDEX idx_knowledge_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE knowledge_document_tags (
    document_id BIGINT      NOT NULL,
    tag         VARCHAR(40) NOT NULL,
    CONSTRAINT fk_tags_document FOREIGN KEY (document_id) REFERENCES knowledge_documents (id) ON DELETE CASCADE,
    INDEX idx_tags_document (document_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
