-- Flyway migration: Add support for hierarchical (tree-like) column structures
-- Adds parent_column_id to table_columns to support parent-child relationships
-- Adds parent_cell_id to cells to support nested cell values

-- Add parent reference to support tree structure in columns
ALTER TABLE table_columns
    ADD COLUMN parent_column_id UUID NULL REFERENCES table_columns (id) ON DELETE CASCADE;

-- Index for efficient tree queries
CREATE INDEX IF NOT EXISTS table_columns_parent_column_id_idx
    ON table_columns (parent_column_id);

-- Add parent cell reference for hierarchical cell values
ALTER TABLE cells
    ADD COLUMN parent_cell_id BIGINT NULL REFERENCES cells (id) ON DELETE CASCADE;

-- Index for efficient cell tree queries
CREATE INDEX IF NOT EXISTS cells_parent_cell_id_idx
    ON cells (parent_cell_id);

-- Drop unique constraint on cells since now we can have multiple cells with same column_definition_id
-- but different parent_cell_id (for hierarchical structures)
DROP INDEX IF EXISTS cells_row_id_column_definition_id_uk;

-- Create new unique constraint that includes parent_cell_id
-- This ensures a row cannot have duplicate cells for the same column at the same hierarchy level
CREATE UNIQUE INDEX IF NOT EXISTS cells_row_id_column_definition_id_parent_uk
    ON cells (row_id, column_definition_id, COALESCE(parent_cell_id, -1));
