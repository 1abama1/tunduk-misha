-- Add instance_number column
ALTER TABLE tool_instances ADD COLUMN instance_number INT;

-- Assign sequential numbers per template_id
WITH numbered AS (
    SELECT id, 
           ROW_NUMBER() OVER(PARTITION BY template_id ORDER BY created_at, id) as rn
    FROM tool_instances
)
UPDATE tool_instances ti
SET instance_number = numbered.rn
FROM numbered
WHERE ti.id = numbered.id;
