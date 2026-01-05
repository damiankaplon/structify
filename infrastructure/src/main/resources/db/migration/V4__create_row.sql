-- Flyway migration: Create schema for Row aggregate (rows, cells)
-- Note: Uses UUIDs for rows; cells use BIGSERIAL surrogate key

CREATE TABLE IF NOT EXISTS rows
(
    id       UUID PRIMARY KEY,
    table_id UUID NOT NULL REFERENCES tables (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS rows_table_id_idx ON rows (table_id);

CREATE TABLE IF NOT EXISTS cells
(
    id        BIGSERIAL PRIMARY KEY,
    row_id    UUID    NOT NULL REFERENCES rows (id) ON DELETE CASCADE,
    column_id INTEGER NOT NULL,
    value     TEXT    NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS cells_row_id_column_id_uk ON cells (row_id, column_id);
CREATE INDEX IF NOT EXISTS cells_row_id_idx ON cells (row_id);
