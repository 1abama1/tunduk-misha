-- Добавляем колонку status в таблицу tool_instances для отслеживания состояния (ремонт, списание и т.д.)
ALTER TABLE tool_instances ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'AVAILABLE';

-- Присваиваем 'AVAILABLE' всем существующим инструментам, у которых status NULL (вдруг есть)
UPDATE tool_instances SET status = 'AVAILABLE' WHERE status IS NULL;

-- Таблица для бронирования инструментов (привязка к модели/шаблону)
CREATE TABLE IF NOT EXISTS tool_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id BIGINT NOT NULL REFERENCES clients(id),
    template_id UUID NOT NULL REFERENCES tool_templates(id),
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tool_bookings_template_dates 
ON tool_bookings (template_id, start_date_time, end_date_time);
