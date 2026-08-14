-- Добавляем расширение для генерации UUID, если не включено (pg 13+ поддерживает gen_random_uuid() из коробки)
-- CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Подготовка таблиц, добавление новых колонок UUID и нужных по бизнес-логике полей
ALTER TABLE tool_categories ADD COLUMN new_id UUID DEFAULT gen_random_uuid();
ALTER TABLE tool_categories ADD COLUMN parent_category_id UUID;
ALTER TABLE tool_categories ADD COLUMN description TEXT;

ALTER TABLE tool_templates ADD COLUMN new_id UUID DEFAULT gen_random_uuid();
ALTER TABLE tool_templates ADD COLUMN new_category_id UUID;
ALTER TABLE tool_templates ADD COLUMN description TEXT;
ALTER TABLE tool_templates ADD COLUMN specifications JSONB;
ALTER TABLE tool_templates ADD COLUMN daily_rental_price DECIMAL(10,2);
ALTER TABLE tool_templates ADD COLUMN deposit_amount DECIMAL(10,2);
ALTER TABLE tool_templates ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tool_templates ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE tools ADD COLUMN new_id UUID DEFAULT gen_random_uuid();
ALTER TABLE tools ADD COLUMN new_template_id UUID;

-- Добавляем колонки в ссылающиеся таблицы (products)
ALTER TABLE products ADD COLUMN new_category_id UUID;
-- rental_documents.tool_id
ALTER TABLE rental_documents ADD COLUMN new_tool_id UUID;

-- 2. Миграция данных (Связи FK)
UPDATE tool_templates tt SET new_category_id = tc.new_id FROM tool_categories tc WHERE tt.category_id = tc.id;
UPDATE tools t SET new_template_id = tt.new_id FROM tool_templates tt WHERE t.template_id = tt.id;
UPDATE products p SET new_category_id = tc.new_id FROM tool_categories tc WHERE p.category_id = tc.id;
UPDATE rental_documents r SET new_tool_id = t.new_id FROM tools t WHERE r.tool_id = t.id;

-- Миграция данных (Перенос цены и залога из tools в tool_templates)
UPDATE tool_templates tt
SET daily_rental_price = (SELECT MAX(daily_price) FROM tools t WHERE t.template_id = tt.id),
    deposit_amount = (SELECT MAX(deposit) FROM tools t WHERE t.template_id = tt.id)
WHERE EXISTS (SELECT 1 FROM tools t WHERE t.template_id = tt.id);

-- 3. Удаление старых таблиц
DROP TABLE IF EXISTS tool_attributes CASCADE;
DROP TABLE IF EXISTS tool_images CASCADE;

-- 4. Переключение Primary Keys и Foreign Keys

-- Сначала удаляем FK-ограничения
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_category_id_fkey;
ALTER TABLE tools DROP CONSTRAINT IF EXISTS tools_template_id_fkey;
ALTER TABLE tool_templates DROP CONSTRAINT IF EXISTS tool_templates_category_id_fkey;

-- Подменяем колонки (Products)
ALTER TABLE products DROP COLUMN category_id;
ALTER TABLE products RENAME COLUMN new_category_id TO category_id;

-- Подменяем колонки (Rental Documents)
ALTER TABLE rental_documents DROP COLUMN tool_id;
ALTER TABLE rental_documents RENAME COLUMN new_tool_id TO tool_id;

-- Подменяем колонки и PK (Tools)
ALTER TABLE tools DROP COLUMN template_id;
ALTER TABLE tools RENAME COLUMN new_template_id TO template_id;
ALTER TABLE tools DROP COLUMN id CASCADE; -- CASCADE удалит индексы и связи, если мы что-то пропустили
ALTER TABLE tools RENAME COLUMN new_id TO id;
ALTER TABLE tools ADD PRIMARY KEY (id);

-- Подменяем колонки и PK (ToolTemplates)
ALTER TABLE tool_templates DROP COLUMN category_id;
ALTER TABLE tool_templates RENAME COLUMN new_category_id TO category_id;
ALTER TABLE tool_templates DROP COLUMN id CASCADE;
ALTER TABLE tool_templates RENAME COLUMN new_id TO id;
ALTER TABLE tool_templates ADD PRIMARY KEY (id);

-- Подменяем колонки и PK (ToolCategories)
ALTER TABLE tool_categories DROP COLUMN id CASCADE;
ALTER TABLE tool_categories RENAME COLUMN new_id TO id;
ALTER TABLE tool_categories ADD PRIMARY KEY (id);

-- Восстанавливаем FK
ALTER TABLE tool_categories ADD CONSTRAINT fk_tc_parent FOREIGN KEY (parent_category_id) REFERENCES tool_categories(id);
ALTER TABLE tool_templates ADD CONSTRAINT fk_tt_category FOREIGN KEY (category_id) REFERENCES tool_categories(id);
ALTER TABLE tools ADD CONSTRAINT fk_t_template FOREIGN KEY (template_id) REFERENCES tool_templates(id);
ALTER TABLE products ADD CONSTRAINT fk_p_category FOREIGN KEY (category_id) REFERENCES tool_categories(id);

-- 5. Чистка таблицы Tools (удаление ненужных колонок согласно заданию)
ALTER TABLE tools 
  DROP COLUMN IF EXISTS name,
  DROP COLUMN IF EXISTS article,
  DROP COLUMN IF EXISTS deposit,
  DROP COLUMN IF EXISTS daily_price,
  DROP COLUMN IF EXISTS serial_number,
  DROP COLUMN IF EXISTS status,
  DROP COLUMN IF EXISTS instance_number;

-- В таблице tools уже есть created_at (по V1), но мы добавим updated_at
ALTER TABLE tools ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 6. Переименование таблицы tools в tool_instances
ALTER TABLE tools RENAME TO tool_instances;
