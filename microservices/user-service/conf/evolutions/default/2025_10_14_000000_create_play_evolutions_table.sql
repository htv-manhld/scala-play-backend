# Create play_evolutions tracking table

# --- !Ups
-- Tạo bảng tracking migrations giống Laravel
CREATE TABLE IF NOT EXISTS play_evolutions (
    id SERIAL PRIMARY KEY,
    migration VARCHAR(255) NOT NULL UNIQUE,
    batch INTEGER NOT NULL DEFAULT 1,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE play_evolutions IS 'Migration tracking table (like Laravel migrations)';
COMMENT ON COLUMN play_evolutions.migration IS 'Migration filename (e.g., 1.sql)';
COMMENT ON COLUMN play_evolutions.batch IS 'Migration batch number';
COMMENT ON COLUMN play_evolutions.applied_at IS 'When the migration was applied';

# --- !Downs
DROP TABLE IF EXISTS play_evolutions;
