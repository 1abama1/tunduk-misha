# Architecture and change boundaries

The source map is maintained in [audit/ARCHITECTURE.md](audit/ARCHITECTURE.md). This remains a single Spring Boot application with controller/service/repository packages. Existing ContractService delegates CRUD, reporting and Excel work; package relocation and new infrastructure were unnecessary for the confirmed defects.

Critical mutation entry points are transactional and acquire RentalWriteLock (PostgreSQL `pg_advisory_xact_lock(74928103)`). The lock covers rental/restore/close, bookings, catalog/client mutations and offline batch processing across application instances sharing the database. It is deliberately global. Every new writer of the same invariants must participate in the lock protocol. Direct SQL writers do not automatically acquire it.

Current assignment is `ToolInstance.contract`. RentalDocument's historicalToolIds and legacy toolId preserve the composition after return. Creating a multi-tool contract validates all instances before assigning any; closing releases each one; restoration validates the complete historical set before changes. Contract statuses are derived: terminatedAt takes precedence, then returnDate, otherwise ACTIVE. No termination endpoint exists in the current controller inventory; changing termination business rules is outside this patch.

The refactor centralizes login identifier normalization, adds explicit repository soft deletion that retains historical collection rows, returns flushed revisions, fixes period availability and completes XLSX tool appendices. No migration, monetary storage type, API URL, database identifier or package boundary is changed.

Open architectural decisions include branch ownership, durable audit storage, pagination for full snapshots and historical financial snapshots. See the risk register; this change does not claim these boundaries are already enforced.
