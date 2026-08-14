DELETE FROM tool_bookings;

ALTER TABLE tool_bookings ADD COLUMN tool_instance_id BIGINT NOT NULL;

ALTER TABLE tool_bookings ADD CONSTRAINT fk_tool_bookings_instance 
    FOREIGN KEY (tool_instance_id) REFERENCES tool_instances(id);
