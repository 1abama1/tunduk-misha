-- Existing versioned migrations are immutable. Refuse destructive legacy upgrades
-- on populated databases; those installations require an archived, verified conversion.
DO $$
DECLARE done6 boolean := false; done11 boolean := false; done12 boolean := false;
        populated boolean; table_name text;
BEGIN
  IF to_regclass('flyway_schema_history') IS NOT NULL THEN
    SELECT EXISTS(SELECT 1 FROM flyway_schema_history WHERE version = '6' AND success),
           EXISTS(SELECT 1 FROM flyway_schema_history WHERE version = '11' AND success),
           EXISTS(SELECT 1 FROM flyway_schema_history WHERE version = '12' AND success)
    INTO done6, done11, done12;
  END IF;
  IF NOT done6 THEN
    FOREACH table_name IN ARRAY ARRAY['tools','tool_categories','tool_templates','tool_attributes','tool_images','rental_documents','products'] LOOP
      IF to_regclass(table_name) IS NOT NULL THEN
        EXECUTE format('SELECT EXISTS(SELECT 1 FROM %I)', table_name) INTO populated;
        IF populated THEN
          RAISE EXCEPTION 'Unsafe legacy V6 upgrade blocked: % contains data. Back up and perform a verified preserving conversion before upgrading.', table_name;
        END IF;
      END IF;
    END LOOP;
  END IF;
  IF (NOT done11 OR NOT done12) AND to_regclass('tool_bookings') IS NOT NULL THEN
    EXECUTE 'SELECT EXISTS(SELECT 1 FROM tool_bookings)' INTO populated;
    IF populated THEN
      RAISE EXCEPTION 'Unsafe V11/V12 upgrade blocked: bookings must be migrated without deletion before upgrading.';
    END IF;
  END IF;
END $$;
