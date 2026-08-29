package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SoftDeleteRetentionJob {

    private final JdbcTemplate jdbcTemplate;

    // Run every 30 days (or day 1 of the month for simplicity)
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void cleanupOldSoftDeletes() {
        log.info("Starting cleanup of old soft-deleted records (> 90 days)");

        String[] tables = {
            "clients", 
            "tool_instances", 
            "tool_templates", 
            "tool_categories", 
            "rental_documents", 
            "tool_bookings"
        };

        for (String table : tables) {
            int deleted = jdbcTemplate.update(
                "DELETE FROM " + table + " WHERE is_deleted = true AND deleted_at < NOW() - INTERVAL '90 days'"
            );
            log.info("Deleted {} old soft-deleted records from table {}", deleted, table);
        }

        log.info("Finished cleanup of old soft-deleted records");
    }
}
