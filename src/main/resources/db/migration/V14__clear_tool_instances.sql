BEGIN;

TRUNCATE TABLE 
  tool_bookings, 
  rental_documents, 
  tool_instances, 
  tool_templates, 
  tool_categories 
RESTART IDENTITY CASCADE;

COMMIT;