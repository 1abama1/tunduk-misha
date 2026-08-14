DELETE FROM tool_bookings;

ALTER TABLE tool_bookings DROP CONSTRAINT tool_bookings_client_id_fkey;
ALTER TABLE tool_bookings DROP COLUMN client_id;

ALTER TABLE tool_bookings ADD COLUMN client_name VARCHAR(255) NOT NULL;
ALTER TABLE tool_bookings ADD COLUMN client_phone VARCHAR(50);
