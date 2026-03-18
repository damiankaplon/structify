-- Flyway migration: Decouple read model tables from domain model tables
-- Creates dedicated read model tables for table, version, and column projections.
-- Migrates existing data from domain tables into the new read model tables.
-- Drops the description column from the domain tables table (it belongs in the read model).

-- 1. Create read model tables

CREATE TABLE table_read_model
(
    id          UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL DEFAULT '',
    PRIMARY KEY (id)
);

CREATE TABLE version_read_model
(
    id           UUID    NOT NULL,
    table_id     UUID    NOT NULL REFERENCES table_read_model (id) ON DELETE CASCADE,
    order_number INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE column_read_model
(
    id               UUID         NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      TEXT         NOT NULL,
    type_name        VARCHAR(50)  NOT NULL,
    string_format    VARCHAR(50)  NULL,
    optional         BOOLEAN      NOT NULL,
    parent_column_id UUID         NULL REFERENCES column_read_model (id) ON DELETE CASCADE,
    PRIMARY KEY (id)
);

CREATE TABLE version_column_read_model
(
    version_id           UUID NOT NULL REFERENCES version_read_model (id) ON DELETE CASCADE,
    column_definition_id UUID NOT NULL REFERENCES column_read_model (id) ON DELETE CASCADE,
    PRIMARY KEY (version_id, column_definition_id)
);

-- 2. Migrate existing data from domain tables into read model tables

INSERT INTO table_read_model (id, user_id, name, description)
SELECT id, user_id, name, COALESCE(description, '')
FROM tables;

INSERT INTO version_read_model (id, table_id, order_number)
SELECT id, table_id, order_number
FROM table_versions;

INSERT INTO column_read_model (id, name, description, type_name, string_format, optional, parent_column_id)
SELECT id, name, description, type_name, string_format, optional, parent_column_id
FROM table_columns;

INSERT INTO version_column_read_model (version_id, column_definition_id)
SELECT version_id, column_definition_id
FROM version_column_assoc;

-- 3. Drop description column from the domain tables table

ALTER TABLE tables
    DROP COLUMN description;
