-- V1__init_schema.sql
-- Initial schema for AI Finance Tracker.
-- Creates all four core domain tables with proper constraints and indexes.
-- Managed by Flyway; never edit this file after it has been applied to any environment.

CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- transaction_type is an enum-like check: only INCOME or EXPENSE are valid.
CREATE TABLE IF NOT EXISTS transactions (
    id          BIGSERIAL       PRIMARY KEY,
    amount      NUMERIC(19, 2)  NOT NULL,
    type        VARCHAR(10)     NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category    VARCHAR(255)    NOT NULL,
    description VARCHAR(1000),
    date        DATE            NOT NULL,
    user_id     BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_id   ON transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date       ON transactions (date);
CREATE INDEX IF NOT EXISTS idx_transactions_user_date  ON transactions (user_id, date);
CREATE INDEX IF NOT EXISTS idx_transactions_type       ON transactions (type);

-- month stored as a 7-char string "YYYY-MM" (e.g. "2024-03")
CREATE TABLE IF NOT EXISTS budgets (
    id           BIGSERIAL       PRIMARY KEY,
    category     VARCHAR(255)    NOT NULL,
    limit_amount NUMERIC(19, 2)  NOT NULL,
    month        VARCHAR(7)      NOT NULL,
    user_id      BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_budget_user_category_month UNIQUE (user_id, category, month)
);

CREATE INDEX IF NOT EXISTS idx_budgets_user_id ON budgets (user_id);
CREATE INDEX IF NOT EXISTS idx_budgets_month   ON budgets (month);

CREATE TABLE IF NOT EXISTS ai_insights (
    id           BIGSERIAL  PRIMARY KEY,
    summary      TEXT       NOT NULL,
    generated_at TIMESTAMP  NOT NULL DEFAULT NOW(),
    user_id      BIGINT     NOT NULL REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_insights_user_id ON ai_insights (user_id);
