# API contracts

[Complete source-derived endpoint/role/request/response/transaction inventory](audit/API_INVENTORY.md).

`/api/admin/*` exposes administrator CRM operations. `/api/v1/admin/sync/*` exposes trader integrations; `/api/v1/sync/*` exposes administrator offline CRM synchronization. Version strings are historical naming, not distinct privilege models.

Actual public authentication endpoints are POST `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`. Registration assigns TRADER. Role.java has ADMIN/TRADER only. `/api/v1/auth/*` and GET refresh are not implemented, despite old whitelist/guide examples. Profile resolves the authenticated subject's own user.

POST `/api/admin/contracts/create` accepts clientId and either toolIds (1–10 distinct positive IDs) or legacy toolId; toolIds takes precedence when populated. Optional offlineId identifies a retriable creation. Response keeps existing fields and includes toolId/toolIds/updatedAt. Number/creation timestamps retain the existing uniqueness policy.

POST close takes optional paidAmount, comment, isBroken, actualReturnDate. A second close is rejected. Restore of a CLOSED contract reacquires all historical tools; terminated, debtor, booked, repair or busy cases are rejected. Money remains JSON numbers and Double in legacy rental APIs; no unreviewed rounding migration was introduced.

ApiError retains `code,message,status,timestamp`. Missing/type-invalid parameters and unreadable JSON return 400; oversized multipart returns 413; database/revision/idempotency conflicts return 409; unexpected failures return a generic 500. Existing filter-generated 401/403 responses remain separate. Broad error-format versioning is deferred.

Legacy POST `/api/tools` still requires name, deposit and dailyPrice in addition to templateId even though stored instance creation uses template metadata/prices. Supply these legacy fields until that contract is separately reconciled. POST `/api/admin/clients/create` requires whatsappPhone. Booking creation/tool batch creation return 201; booking/tool deletion return 204; other success statuses are in the inventory.

Trader product/order/category sync pages are capped at 200 and ordered by id. Invalid product/order `since` is now 400, not an implicit full response; database failures propagate to error handling. Per-record `version` is rejected as an unsafe global cursor. Category sync remains a full paged catalog regardless of since. See [sync](SYNC_PROTOCOL.md) for offline snapshots and retry behavior.

Image downloads permit inline PNG/JPEG/GIF/WebP only. Other media types are downloaded as octet-stream with nosniff; filename control characters are removed. This avoids executing uploaded HTML/SVG on the API origin while keeping existing uploads downloadable.
