CREATE TABLE IF NOT EXISTS roles
(
    id
    SERIAL
    PRIMARY
    KEY,
    role_name
    VARCHAR
(
    50
) NOT NULL UNIQUE
    );