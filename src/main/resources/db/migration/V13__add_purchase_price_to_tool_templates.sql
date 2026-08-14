ALTER TABLE tool_templates
ADD COLUMN purchase_price NUMERIC(19, 2);

UPDATE tool_templates tt
SET purchase_price = (SELECT MAX(purchase_price) FROM tool_instances t WHERE t.template_id = tt.id)
WHERE EXISTS (SELECT 1 FROM tool_instances t WHERE t.template_id = tt.id);

ALTER TABLE tool_instances DROP COLUMN IF EXISTS purchase_price;
