CREATE TABLE branches (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE rental_points (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    address     TEXT,
    phone       VARCHAR(255),
    email       VARCHAR(255)
);

CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    full_name           VARCHAR(255),
    birth_date          DATE,
    address_living      VARCHAR(255),
    address_registration VARCHAR(255),
    email               VARCHAR(255) UNIQUE,
    phone               VARCHAR(255) UNIQUE,
    password_hash       VARCHAR(255),
    consent_personal_data BOOLEAN NOT NULL DEFAULT FALSE,
    consent_privacy_policy BOOLEAN NOT NULL DEFAULT FALSE,
    consent_user_agreement  BOOLEAN NOT NULL DEFAULT FALSE,
    simple_mode          BOOLEAN NOT NULL DEFAULT FALSE,
    role                VARCHAR(255) NOT NULL DEFAULT 'ADMIN'
);

CREATE TABLE user_tags (
    user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tag       VARCHAR(255)
);

CREATE TABLE refresh_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT REFERENCES users(id),
    jti          VARCHAR(255) NOT NULL UNIQUE,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE UNIQUE INDEX idx_refresh_jti ON refresh_tokens(jti);

CREATE TABLE traders (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE REFERENCES users(id),
    api_key     VARCHAR(64) UNIQUE,
    approved    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE,
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE TABLE tool_categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE tool_templates (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES tool_categories(id)
);

CREATE TABLE clients (
    id               BIGSERIAL PRIMARY KEY,
    full_name         VARCHAR(255),
    phone             VARCHAR(255) UNIQUE,
    email             VARCHAR(255) UNIQUE,
    whatsapp_phone    VARCHAR(255),
    reg_region        VARCHAR(255),
    reg_street        VARCHAR(255),
    live_region       VARCHAR(255),
    live_street       VARCHAR(255),
    object_address    VARCHAR(255),
    birth_date        DATE,
    birth_year        INTEGER,
    passport_number   VARCHAR(255),
    passport_issued_at DATE,
    pin               VARCHAR(255),
    comment           TEXT,
    tag               VARCHAR(255),
    last_branch_id    BIGINT REFERENCES branches(id)
);

CREATE TABLE client_tags (
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    tags      VARCHAR(255)
);

CREATE TABLE client_passports (
    id                BIGSERIAL PRIMARY KEY,
    series            VARCHAR(255),
    number            VARCHAR(255),
    issued_by         VARCHAR(255),
    subdivision_code  VARCHAR(255),
    issue_date        DATE,
    inn               VARCHAR(20),
    client_id         BIGINT NOT NULL UNIQUE REFERENCES clients(id)
);

CREATE TABLE rental_documents (
    id                 BIGSERIAL PRIMARY KEY,
    contract_number    VARCHAR(255) NOT NULL,
    start_date_time    TIMESTAMP,
    daily_price        DOUBLE PRECISION,
    amount             DOUBLE PRECISION,
    comment            VARCHAR(255),
    client_id          BIGINT NOT NULL REFERENCES clients(id),
    created_at         TIMESTAMP,
    return_date        TIMESTAMP,
    terminated_at      TIMESTAMP,
    termination_reason VARCHAR(255),
    tool_id            BIGINT,
    updated_at         TIMESTAMP,
    offline_id         VARCHAR(255),
    CONSTRAINT uk_contract_number_created_at UNIQUE (contract_number, created_at)
);

CREATE TABLE tools (
    id               BIGSERIAL PRIMARY KEY,
    inventory_number VARCHAR(255) UNIQUE,
    instance_number  INTEGER,
    serial_number    VARCHAR(255),
    status           VARCHAR(255) NOT NULL DEFAULT 'AVAILABLE',
    template_id      BIGINT NOT NULL REFERENCES tool_templates(id),
    contract_id      BIGINT REFERENCES rental_documents(id),
    name             VARCHAR(255),
    article          VARCHAR(255),
    deposit          DOUBLE PRECISION,
    purchase_price   DOUBLE PRECISION,
    daily_price      DOUBLE PRECISION,
    branch_id        BIGINT REFERENCES branches(id),
    rental_point_id  BIGINT REFERENCES rental_points(id),
    created_at       TIMESTAMP NOT NULL
);

CREATE TABLE tool_attributes (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    value   TEXT NOT NULL,
    tool_id BIGINT NOT NULL REFERENCES tools(id) ON DELETE CASCADE
);

CREATE TABLE tool_images (
    id           BIGSERIAL PRIMARY KEY,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    data         BYTEA NOT NULL,
    tool_id      BIGINT NOT NULL REFERENCES tools(id) ON DELETE CASCADE
);

CREATE TABLE client_images (
    id         BIGSERIAL PRIMARY KEY,
    file_name  VARCHAR(255),
    file_type  VARCHAR(255),
    data       BYTEA NOT NULL,
    client_id  BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE
);

CREATE TABLE products (
    id           BIGSERIAL PRIMARY KEY,
    external_id  VARCHAR(255) NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(255),
    sku          VARCHAR(255),
    price        DOUBLE PRECISION,
    stock        INTEGER,
    trader_id    BIGINT NOT NULL REFERENCES traders(id),
    category_id BIGINT REFERENCES tool_categories(id),
    version      BIGINT NOT NULL DEFAULT 1,
    created_at   TIMESTAMP WITH TIME ZONE,
    updated_at   TIMESTAMP WITH TIME ZONE
);

CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    external_id   VARCHAR(255) NOT NULL UNIQUE,
    trader_id     BIGINT NOT NULL REFERENCES traders(id),
    product_id    BIGINT REFERENCES products(id),
    status        VARCHAR(255) NOT NULL,
    quantity      INTEGER,
    total_amount  DOUBLE PRECISION,
    version       BIGINT NOT NULL DEFAULT 1,
    created_at    TIMESTAMP WITH TIME ZONE,
    updated_at    TIMESTAMP WITH TIME ZONE
);
