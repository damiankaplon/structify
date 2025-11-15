-- Flyway migration: Create schema for Table aggregate (tables, versions, columns)
-- Note: Uses UUIDs for aggregate roots and versions; columns use BIGSERIAL surrogate key

CREATE TABLE IF NOT EXISTS tables (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS table_versions (
    id UUID PRIMARY KEY,
    table_id UUID NOT NULL REFERENCES tables(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    order_number INTEGER NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS table_versions_table_id_order_number_uk
    ON table_versions(table_id, order_number);

CREATE TABLE IF NOT EXISTS table_columns (
    id BIGSERIAL PRIMARY KEY,
    version_id UUID NOT NULL REFERENCES table_versions(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    type_name VARCHAR(50) NOT NULL,
    string_format VARCHAR(50) NULL,
    optional BOOLEAN NOT NULL
);

CREATE INDEX IF NOT EXISTS table_columns_version_id_idx ON table_columns(version_id);
