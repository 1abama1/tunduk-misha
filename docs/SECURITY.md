# Security behavior and limits

Read [the route matrix](audit/API_INVENTORY.md) and [risk register](audit/RISK_REGISTER.md) before release. SecurityRoutesTest runs the actual SecurityFilterChain with role and bearer-token cases; older MVC smoke tests disable filters and do not prove authorization.

Passwords use BCrypt. Registration, login facade and authentication provider now use the same Locale.ROOT email and phone normalization. Existing stored identifiers are not rewritten. Registration issues TRADER privileges, never ADMIN. Bootstrap credentials and signing secrets come from environment; JWT HMAC key construction requires at least 32 UTF-8 bytes. The secret is used as raw UTF-8 text, not decoded Base64.

Access tokens expire after 900000 ms (15 minutes), refresh tokens after 604800000 ms (7 days). JwtFilter verifies signature/expiry/type/positive numeric subject and reads subject/role from one validated parse. A refresh token cannot authenticate an API request. Invalid-token details are not logged. Malformed nonnumeric API-key principals fail closed; keys must be approved and belong to the authenticated user. ADMIN selects a trader through traderId; supplying an API key still enforces key ownership.

Refresh entries store a random JTI, not the bearer JWT. Rotation locks the refresh row in a transaction, validates stored ownership and server expiry, revokes the old token, and inserts the replacement atomically. Reuse deletes that user's refresh entries and commits the invalidation despite the 401. Two competing refreshes have at most one successful rotation; the losing replay invalidates the winner's new refresh token. Clients must serialize refresh calls. Access tokens issued before this event remain valid until expiry.

Role changes/deletion are not immediately applied to issued access tokens: authorization trusts the signed role until the 15-minute expiry. No DB lookup per API request was added. Immediate revocation would require an agreed session/version/cache design. Users have no enabled/disabled field in the present model. Cross-session refresh revocation racing with a new login is not exhaustively characterized.

CSRF is disabled for the existing stateless bearer scheme; tokens are returned in JSON, not authentication cookies. If consumers introduce cookie authentication, revisit CSRF. CORS currently permits localhost/127.0.0.1 origin patterns with credentials; production frontend origins require explicit configuration work. Login throttling/brute-force control is not implemented here; enforce a reviewed gateway policy before public exposure.

Multipart limits remain 10 MB/file, 30 MB/request. Active uploaded content is served as attachment; image bytes are not fully decoded/validated at upload. POI 5.5.1 replaces vulnerable 5.2.5 ([Apache advisory](https://poi.apache.org/), CVE-2025-31672). XLSX tests use the actual template and verify user text remains STRING, including on pre-existing formula cells.

Dependency analysis is partial, not a clean vulnerability attestation. Official [Spring advisories](https://spring.io/security/) were checked. [CVE-2026-41707](https://spring.io/security/cve-2026-41707/) concerns DPoP and [CVE-2026-47841](https://spring.io/security/cve-2026-47841/) concerns WebAuthn/distributed sessions; those features are absent from this app. This does not establish that all advisories are inapplicable. Boot remains 3.5.6, JJWT 0.11.5 and Lombok 1.18.32; run a complete current SCA scan and evaluate compatible maintenance updates before release.

AuditLogService still writes stdout before transaction commit. It is not a durable, committed-events-only audit trail. Configure log access/retention; designing durable audit/outbox storage remains open.

Trader category JSON retains catalog metadata/templates and one hierarchy level, and excludes traversal into template instances/rental clients and ORM proxy internals. The previous recursive graph failed at Jackson depth 1001. Consumer searches in the adjacent frontend found no trader category-sync usage, but external consumers of recursive nested graphs remain unknown and require release verification.
