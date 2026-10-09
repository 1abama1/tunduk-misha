package org.misha.authservice.service;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
@Component @RequiredArgsConstructor
public class RentalWriteLock {
    private final JdbcTemplate jdbc;
    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire() {
        jdbc.query("SELECT pg_advisory_xact_lock(74928103)", rs -> { return null; });
    }
}
