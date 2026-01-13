ALTER TABLE cells
    DROP CONSTRAINT IF EXISTS cells_row_id_column_id_uk;

ALTER TABLE cells
    ADD COLUMN column_definition_id uuid;

ALTER TABLE table_columns
    ADD id_uuid uuid NOT NULL DEFAULT gen_random_uuid();

WITH cells_collumns AS
         (SELECT cls.id AS cell_id, c.id_uuid AS column_uuid
          FROM cells cls
                   JOIN table_columns c ON cls.column_id = c.id)
UPDATE cells
SET column_definition_id = column_uuid
FROM cells_collumns
WHERE cells.id = cells_collumns.cell_id;

ALTER TABLE cells
    ALTER COLUMN column_definition_id SET NOT NULL;

ALTER TABLE table_columns
    DROP CONSTRAINT IF EXISTS table_columns_version_id_name_uk;

ALTER TABLE table_columns
    DROP COLUMN id;

ALTER TABLE table_columns
    DROP CONSTRAINT IF EXISTS table_columns_pkey;

ALTER TABLE table_columns
    RENAME COLUMN id_uuid TO id;

ALTER TABLE table_columns
    ADD PRIMARY KEY (id);


ALTER TABLE cells
    ADD CONSTRAINT cells_column_definition_id_fk FOREIGN KEY (column_definition_id) REFERENCES table_columns (id);

ALTER TABLE cells
    DROP COLUMN column_id;

ALTER TABLE rows
    ADD COLUMN version_id uuid REFERENCES table_versions (id);

WITH table_current_version AS (SELECT t.id, MAX(v.order_number) AS max_order
                               FROM tables t
                                        JOIN table_versions v ON t.id = v.table_id
                               GROUP BY t.id)
UPDATE rows
SET version_id = (SELECT tv.id
                  FROM table_versions tv
                  WHERE tv.table_id = table_current_version.id
                    AND tv.order_number = table_current_version.max_order)
FROM table_current_version
WHERE rows.table_id = table_current_version.id;

ALTER TABLE rows
    ALTER COLUMN version_id SET NOT NULL;

ALTER TABLE rows
    DROP COLUMN table_id;

CREATE UNIQUE INDEX IF NOT EXISTS cells_row_id_column_definition_id_uk ON cells (row_id, column_definition_id);

CREATE TABLE version_column_assoc
(
    version_id           uuid NOT NULL REFERENCES table_versions (id),
    column_definition_id uuid NOT NULL REFERENCES table_columns (id),
    PRIMARY KEY (version_id, column_definition_id)
);

UPDATE version_column_assoc
SET version_id           = c.version_id,
    column_definition_id = c.id
FROM table_columns c;

ALTER TABLE table_columns
    DROP COLUMN version_id;
