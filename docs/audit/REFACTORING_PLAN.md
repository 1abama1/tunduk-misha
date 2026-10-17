# Execution plan and release boundaries

1. Capture original build/test results, mapping inventory and migration hashes. Isolate tests from `.env` and business data before more context launches.
2. Write behavior tests for refresh ownership/expiry/replay, API-key binding, revision response consistency, invalid compositions, sync validation, security route matrix, file generation and idempotency.
3. Apply minimal confirmed fixes. Preserve signed-role access tokens (15 minute revocation lag), global advisory lock, snapshot mode, URLs, JSON money types and existing Flyway checksums.
4. Verify PostgreSQL semantics using a disposable container or explicitly configured isolated test schema; never substitute Mockito or H2 for database evidence. Keep missing infrastructure visible as a failed full verification.
5. Add fail-closed CI, reproducible wrapper invocation, container readiness/non-root runtime, deployment/testing/security/sync documentation.
6. Re-run checks, inspect the complete diff and record unresolved business decisions and environment limitations in the risk register and final report.

Deferred pending requirements/data: branch authorization and ownership model; BigDecimal migration and rounding; durable audit retention; contract-number uniqueness policy; restore/termination semantics; incremental sync cursor; production performance tuning. Do independent safe fixes while these remain open.
