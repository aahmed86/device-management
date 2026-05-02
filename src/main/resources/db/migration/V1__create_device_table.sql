CREATE TABLE device (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    brand      VARCHAR(255) NOT NULL,
    state      VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    version    BIGINT
);