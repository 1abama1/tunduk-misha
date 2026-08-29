ALTER TABLE clients 
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

ALTER TABLE tool_instances 
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

ALTER TABLE tool_templates 
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

ALTER TABLE tool_categories 
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

ALTER TABLE rental_documents 
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

ALTER TABLE tool_bookings 
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

-- Индексы для быстрой выборки дельт
CREATE INDEX IF NOT EXISTS idx_clients_sync ON clients (last_branch_id, updated_at, deleted_at);
CREATE INDEX IF NOT EXISTS idx_tool_instances_sync ON tool_instances (branch_id, updated_at, deleted_at);
CREATE INDEX IF NOT EXISTS idx_tool_templates_sync ON tool_templates (updated_at, deleted_at);
CREATE INDEX IF NOT EXISTS idx_tool_categories_sync ON tool_categories (updated_at, deleted_at);
CREATE INDEX IF NOT EXISTS idx_rental_docs_sync ON rental_documents (updated_at, deleted_at);
CREATE INDEX IF NOT EXISTS idx_tool_bookings_sync ON tool_bookings (updated_at, deleted_at);
