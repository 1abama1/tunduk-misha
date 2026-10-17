# Engineering risk register — 2026-10-17

CONFIRMED = demonstrated by code/test; PROBABLE = evidence suggests risk, deployment/runtime confirmation needed; HYPOTHESIS = unverified. Severity describes demonstrated impact, not speculative worst cases. No confirmed exploitable P0 was established. Verification summaries refer to the final full suite unless noted.

| ID | Severity | Evidence / impact | Confidence | Status |
| --- | --- | --- | --- | --- |
| R01 | P1 | Original context test loads .env and migrates local business DB | CONFIRMED, actual incident | Fixed test isolation; owner review of initial V17 application required |
| R02 | P1 | ORM removal deletes rental_document_tools before soft UPDATE | CONFIRMED PostgreSQL test failed before fix | Fixed supported repository/service delete paths |
| R03 | P1 | Deleted-contract filter bypasses client-history deletion guard | CONFIRMED source + regression | Fixed native all-history check |
| R04 | P1 | Several bookings of one instance/repair bookings undercount availability | CONFIRMED PostgreSQL expected 1, got 0 | Fixed distinct candidate query |
| R05 | P1 | Contract Excel truncates tools 6–10; cached formula results overwritten on recalc | CONFIRMED real-template test | Fixed complete appendices and literal cell writes |
| R06 | P1 | Pre-flush revision response differs from persisted updatedAt | CONFIRMED unit reproduction + PostgreSQL equality | Fixed create/update flush; batch preflight adjusted |
| R07 | P2 | Refresh checks omit stored owner/expiry/JTI validation | CONFIRMED missing checks, exploitability not demonstrated | Hardened |
| R08 | P2 | Nonnumeric API-key principal skips ownership check; null approval NPE | CONFIRMED unit reproduction; normal issuer already uses numeric subjects | Fixed fail-closed handling |
| R09 | P1 | POI 5.2.5 affected by CVE-2025-31672 | CONFIRMED Apache advisory/version; exploit not attempted | Updated to 5.5.1, roundtrip tests pass |
| R10 | P1 | Category→template/category and parent/child recursion exceeds JSON depth | CONFIRMED serializer reproduction | Fixed bounded relationship traversal |
| R11 | P1 | Uploaded active content returned inline under caller-supplied MIME | CONFIRMED source/header regression | Fixed safe inline image allowlist; other types attachment |
| R12 | P2 | Unbounded trader page size, broad catch hides DB errors as full sync | CONFIRMED source/tests | Fixed cap/order/narrow parse handling |
| R13 | P2 | Null batch items / malformed parameters lead to 500; invalid tool composition late errors | CONFIRMED source/tests | Fixed validation and error handlers |
| R14 | P2 | Windows wrapper null Target indexing; old mocks fail baseline | CONFIRMED executed commands | Fixed wrapper/mock wiring |
| R15 | P2 | Repair count is always 0 despite IN_REPAIR model | CONFIRMED source + test | Fixed repository count |
| R16 | P2 | Audit is stdout, includes transactions that later roll back | CONFIRMED source | Open: durable audit design/retention |
| R17 | P2 | Global snapshot ignores since/branchId; no branch ownership on contracts | CONFIRMED source; not a demonstrated global-ADMIN authorization bypass | Documented existing protocol; branch product needs requirements |
| R18 | P2 | Double money, current catalog values used in historical reports | CONFIRMED types/source; historical repricing impact needs requirements | Open compatibility/data policy |
| R19 | P2 | Global write serialization and unbounded full snapshots/history; N+1 paths | CONFIRMED code patterns, production bottleneck PROBABLE | Open measurements/protocol design |
| R20 | P2 | Role/delete changes do not revoke issued access JWT immediately | CONFIRMED design, 15-minute TTL | Documented; revocation requirement open |
| R21 | P2 | No app rate limiting; production CORS/health/SCA/container runtime unverified | CONFIRMED app omissions; gateway mitigations unknown | Open deployment gates |
| R22 | P2 | Legacy hard-purge method/bulk ORM removal unsafe for retained history | CONFIRMED code; no scheduled/exposed caller found | Do not invoke; archival policy required |
| R23 | P2 | Other DTO/Excel/import/search edge cases not exhaustively characterized | HYPOTHESIS / review coverage limitation | Further focused audit needed |

## Fixed issue details

### R01 — test isolation

File/method: AuthServiceApplicationTests.contextLoads. Root cause: no test datasource override plus automatic spring-dotenv loading. Business impact: migration/initializers can mutate business data. Solution: PostgresTestSupport requires a container or explicit `_test` database, random schemas, and overrides credentials/signing/bootstrap values. Alternative: fully separate CI service database. Compatibility risk: Docker required by default; external test URL is explicit. Required tests: actual context startup, migrations and dedicated-server execution. Verification: full suite on independent native PostgreSQL. Status: code fixed; first V17 business-database application remains an incident to review, not silently reversed.

### R02/R03 — historical data retention

Files: RentalDocumentRepository.delete/deleteById/markDeleted, ClientService.delete/hasClientHistory. Root causes: ORM collection removal precedes custom entity soft deletion; @SQLRestriction hides historical contracts from the guard. Business impact: losing membership/passport data needed for history. Solutions: explicit native soft UPDATE, all-history EXISTS. Alternatives: first-class immutable history entities or separately designed Hibernate soft-delete mapping. Compatibility risk: native update clears the persistence context after flushing; supported service deletion is terminal in its transaction. Do not use bulk removal/EntityManager.remove as equivalent operations. Required tests: closed multi-tool soft delete retains join rows, deleted contract absent from snapshot, historical client cannot be deleted. Verification: PostgreSQL regression. Status: fixed supported paths; R22 tracks unsupported purge/bulk operations.

### R04 — period availability

Files: ToolAvailabilityService.getAvailableCount(period), ToolInstanceRepository.countAvailableForPeriod. Root cause: subtracting booking record counts from instance counts double-counts a busy instance and subtracts bookings on unusable tools. Business impact: falsely reports no inventory. Solution: count AVAILABLE candidates with no active overlapping rental/booking. Alternative: distinct ID sets in Java (larger loads). Compatibility risk: invalid/inverted periods now return 400; booking intervals remain half-open. Required tests: two non-overlapping bookings on one candidate across a wider request; repair booking; adjacent/overlapping bookings; concurrent assignment. Verification: PostgreSQL tests. Status: fixed; no latency gain claimed.

### R05 — Excel completeness

Files: ExcelContractMapper, ExcelGeneratorService.fillToolPriceSheet/set. Root causes: two five-tool caps; POI setCellValue on existing formulas changes cached results only. Business impact: incomplete or recalculated wrong equipment appendices. Solution: carry all tools, additional five-row appendix sheets, clear old formula before literal writes. Alternative: dynamically expand merged table rows with complex print-layout effects. Compatibility risk: contracts above five tools have additional sheets; print/legal-template approval still required; historic deposit/price semantics intentionally unchanged. Required tests: reopen real XLSX, assert ten unique tools, literal cells and untrusted formula-looking client text. Verification: POI roundtrip passes. Status: fixed; visual Excel/PDF print review remains a release gate.

### R06 — revisions

Files: ContractCrudService.createContract/update, SyncService.syncContracts. Root cause: DTO created before Hibernate @PreUpdate flush. Business impact: an immediate legitimate client update conflicts with a revision it just received. Solution: flush before returning revision; compare batch preconditions before mutating updates/closures. Alternative: explicit numeric revision/version migration, requiring protocol work. Compatibility risk: batch can update then close against the same initial revision; optional legacy preconditions retained. Required tests: response equals persisted revision, stale batch rollback, update+close initial revision. Verification: unit/PostgreSQL. Status: fixed.

### R07/R08 — authentication defenses

Files: AuthFacade.refresh, ApiKeyValidationService.validateApiKey, JwtUtil/JwtFilter, LoginIdentifiers. Root causes: incomplete validation and a swallowed NumberFormatException; normalization differed between registration/login. Business impact: bad/corrupt token records or malformed principals are not rejected consistently; mixed-case/formatted identifiers can fail authentication. Solutions: validate JTI, stored owner/expiry, numeric subjects and positive TTLs; fail closed; one signed-claims parse; shared Locale.ROOT normalization. Alternative: DB-backed access sessions/immediate revocation (higher per-request cost). Compatibility risk: malformed signed claims now fail; valid issued claims/URLs stay compatible; no stored identifier migration. Required tests: refresh owner/expiry/reuse, actual row-lock race, API-key owner/null approval, JWT expiry/type/signature/subject, HTTP role matrix. Verification: dedicated unit/security/PostgreSQL tests. Status: hardened; no claim that mismatched JTI was externally forgeable before fix.

### R09 — POI advisory

File: pom.xml. Evidence: [Apache](https://poi.apache.org/) identifies OOXML duplicate-ZIP-entry weakness before 5.4.0. Root cause: pinned 5.2.5. Business impact: inconsistent interpretation of crafted imported workbooks. Solution: same-major 5.5.1 for both POI modules. Alternative: reject external imports, incompatible with existing feature. Compatibility risk: stricter malformed workbook handling/transitive library changes. Required tests: compile, real-template roundtrip and runtime package. Verification: full build; exploit file not constructed. Status: version exposure fixed; exhaustive SCA remains open.

### R10/R11 — response boundaries

Files: ToolCategory/ToolTemplate JSON annotations; AdminClientController.getImage. Root causes: entity graph serialization and caller-controlled MIME/filename headers. Business impact: catalog sync fails with JSON depth >1000; uploaded HTML could execute in API origin if navigated. Solutions: bound category/template graph (retain root fields, templates and one hierarchy level, omit template instance/rental traversal/proxy metadata); safe raster MIME allowlist, attachment fallback and nosniff. Alternatives: dedicated versioned catalog DTOs and decoded/validated image intake. Compatibility risk: consumers of arbitrarily recursive nested categories must adapt; no matching trader category consumers were found in the adjacent frontend checkout, external consumers remain unknown. Required tests: populated cyclic graph serializes without instances/client data, active upload downloads safely. Verification: source regression before/after and MVC. Status: fixed response boundary; full frontend/third-party contract verification remains required.

### R12/R13/R14/R15 — bounded requests, validation and reliability

Files: Product/Order/CategorySyncService; ContractSyncDto; ContractCrudService; GlobalExceptionHandler; mvnw.cmd; StatsService and test wiring. Root causes: unlimited page sizes, broad exception fallback, absent element constraints/parameter mapping, indexing absent link target, stale mocks, hardcoded zero count. Business impact: excess memory, hidden DB failures, misleading 500/404, failed builds and misleading dashboard count. Solutions: cap 200/stable id ordering, parse-specific exception handling, reject invalid compositions/null batch items, correct 400/413 mappings, robust wrapper, missing mock dependencies, actual IN_REPAIR count. Alternatives: full cursor/API/error redesign is unnecessary here. Compatibility risk: invalid since is now 400; excessive pages capped; valid HTTP contracts preserved. Required tests: trader failure/cap tests, invalid HTTP parameters/batches, composition checks, wrapper -v, stats count, full existing suite. Verification: suite and wrapper command. Status: fixed.

## Open issue details and release decisions

### R16 — committed durable audit

File/method: AuditLogService.log*. Evidence/root cause: log.info runs before business commit; no audit table or durable event sink. Business impact: rollback can look like success and local logs can disappear. Proposed solution: transactional persisted events or outbox plus approved access/retention. Alternative: existing centralized logs with clearly marked attempted/committed events. Compatibility risk: schema/logging/personal-data retention policy. Required tests: audit/business atomic rollback, access and retention. Verification: source only; no durable guarantee claimed. Status: open P2, needs retention/operations decisions.

### R17 — branch/snapshot protocol

Files: SyncService.pullSync, RentalDocument/ToolInstance mappings. Evidence/root cause: deliberate full snapshot and absent branch ownership. Business impact: unbounded global snapshot; cannot safely offer branch-limited accounts. Proposed solution: explicit branch ownership/backfill and negotiated snapshot/pagination protocol. Alternative: keep global ADMIN-only snapshots, current behavior. Compatibility risk: silently filtering would make full-snapshot consumers delete locally cached records. Required tests: branch ownership migration/access matrix and atomic cache replacement. Verification: existing global snapshot/deletion regression only. Status: documented current contract; branch change blocked by missing requirements, not an established privilege bypass.

### R18 — money/history semantics

Files: RentalDocument.amount/dailyPrice, report/Excel mappers. Evidence/root cause: Double amounts and template-derived current prices. Business impact: binary rounding/historical repricing possible; actual accounting rules/data distribution unknown. Proposed solution: analyze stored values and agree monetary scale/rounding plus price snapshots, then additive migration. Alternative: retain current model with explicit limitations. Compatibility risk: JSON, SQL, old contracts, Excel and frontend. Required tests: historical data migration, rounding/boundaries, exports. Verification: finite/nonnegative payment tests pass; no money model migration. Status: open P2.

### R19 — measured performance

Files: RentalWriteLock, full sync, reporting/search/mappers. Evidence: global lock; findAll and per-document lookups; production impact unmeasured. Root cause: correctness-first global coordination/legacy list contracts. Business impact: contention, large memory/response size and N+1 at scale are PROBABLE. Proposed solution: production-shaped load/query/lock baseline before per-tool locks or pagination. Alternative: retain current protocol and correctness lock, as here. Compatibility risk: weaker locking loses rental invariants; partial snapshots can lose cache data. Required tests: query counts, EXPLAIN, percentile latency, concurrent multi-instance load. Verification: functional two-contender races only. Status: open P2; no speedup percentage claimed.

### R20 — revocation policy

Files: JwtFilter/JwtUtil/AuthFacade. Evidence/root cause: signed role is trusted until 15-minute expiry; replay removes refresh rows only. Business impact: role/deletion changes have bounded authorization lag. Proposed solution: agree immediate revocation requirements, session version/cache strategy if needed. Alternative: accept documented TTL and serialize refresh clients. Compatibility risk: new per-request DB/cache dependency. Required tests: role/user changes, concurrent sessions/login/replay policy. Verification: current-token and refresh guarantees tested; immediate access revocation absent. Status: accepted existing design pending requirements.

### R21 — delivery/security gates

Files: SecurityConfig, Dockerfile, Compose, CI, dependencies. Evidence: application throttling/readiness/complete SCA absent; Docker daemon unavailable locally. Proposed solution: reviewed gateway controls, production CORS, complete scanner and container CI run. Alternative: internal-only controlled deployment while gates remain unmet. Compatibility risk: remote DB clients need an override for loopback publishing; deployment origin policies may affect frontend. Required tests: real Docker build/start/stop, health DB outage, rate controls, complete SCA and image scan. Verification: Compose template parsed, full Java17 and Java21 Maven suite/package pass, Java17 JAR HTTP smoke passes; GitHub workflow/container runtime not executed. Status: open environment/deployment gates, not production certification.

### R22/R23 — remaining audit scope

Files: SoftDeleteRetentionJob.cleanupOldSoftDeletes; generic bulk repository methods; file import/date/search/card/report edge cases. Root cause: retained hard-purge implementation, ORM removal semantics, historical overlapping APIs. Business impact: retained history could be damaged if unsupported purge/bulk operations are introduced; other edge cases remain hypotheses. Proposed solution: explicit archival graph/policy and focused behavior tests before new callers; continue file/DTO/business-boundary audit. Alternative: leave purge unscheduled and restrict changes to tested supported services. Compatibility risk: deleting old methods would violate the instruction to preserve functionality without consumer analysis. Required tests: archival graph, image/file decoding, roundtrip imports, historical DTOs, booking working-hour rules. Verification: static scan found no scheduled purge or current rental bulk-deletion caller; these areas are not exhaustively verified. Status: open P2 audit scope, explicitly not called complete.
