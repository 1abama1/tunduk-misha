# Observed architecture

Layered Spring Boot modular monolith, package-by-layer. Keep package names and HTTP URLs stable; package relocation has no demonstrated benefit in this change.

| Boundary | Flow / persistence |
| --- | --- |
| Authentication | AuthController → AuthFacade (transaction) → AuthService / AuthenticationManager → users, refresh_tokens. JWT signature and role claims in JwtFilter. |
| CRM clients | AdminClientController → AdminClientService → ClientService / directory / card / image services → clients, passports, tags, images. `/api/clients` is a second DTO contract. |
| Catalog | Category / template / instance controllers → services → category UUID, template UUID, physical instance bigint. |
| Rental | ContractController → ContractService → ContractCrudService / reporting / Excel. RentalController and RentalDocumentController retain legacy use cases. |
| Concurrency | RentalWriteLock acquires one transaction-scoped PostgreSQL advisory lock for rental/catalog/client/sync mutations. Keep this correctness boundary; contention is unmeasured. |
| History | tool_instances.contract_id is current assignment. rental_document_tools and legacy rental_documents.tool_id preserve composition after return. |
| Booking | BookingService → availability / working-hours calculator → tool_bookings. Active bookings reserve a concrete instance; half-open overlap checks. |
| Offline | SyncController → IdempotencyService → SyncService → ContractService. Atomic batch, optional timestamp preconditions, offline IDs, full repeatable-read snapshot. `since` deliberately unused; `branchId` unused and branch ownership absent from contract entity. |
| Trader sync | ADMIN/TRADER route + SyncTraderResolver + API key ownership → trader-scoped product/order repositories; category sync shared. |
| Files/integrations | POI uses classpath XLSX templates; debtor service constructs a browser URL and performs no network I/O. |
| Operations | Flyway + Hibernate validate; required environment secrets; stdout audit logging. No durable audit store, metrics, tracing or CI at baseline. |

All existing controller mappings are inventoried in `API_INVENTORY.md`. Current roles in Role.java are ADMIN and TRADER; CLIENT is a hypothetical denied authority, not a registration option.
