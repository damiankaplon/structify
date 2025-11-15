CREATE UNIQUE INDEX IF NOT EXISTS table_columns_version_id_name_uk
    ON table_columns(version_id, name);
