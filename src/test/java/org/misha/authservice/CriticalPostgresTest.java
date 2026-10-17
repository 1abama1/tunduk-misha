package org.misha.authservice;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.misha.authservice.dto.*;
import org.misha.authservice.entity.*;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.*;
import org.misha.authservice.security.JwtUtil;
import org.misha.authservice.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CriticalPostgresTest extends PostgresTestSupport {
    @Autowired ContractCrudService contracts;
    @Autowired ClientService clientService;
    @Autowired BookingService bookings;
    @Autowired ToolAvailabilityService availability;
    @Autowired SyncService sync;
    @Autowired IdempotencyService idempotency;
    @Autowired AuthFacade auth;
    @Autowired AuthService authService;
    @Autowired JwtUtil jwt;
    @Autowired ClientRepository clients;
    @Autowired ToolCategoryRepository categories;
    @Autowired ToolTemplateRepository templates;
    @Autowired ToolInstanceRepository tools;
    @Autowired RentalDocumentRepository documents;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test void periodAvailabilityCountsDistinctUsableInstances() {
        var f = fixture(3);
        var start = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0);
        bookings.createBooking(new CreateBookingRequest("repair", null, f.templateId, f.ids.get(0), start, 1, null));
        jdbc.update("UPDATE tool_instances SET status='IN_REPAIR' WHERE id=?", f.ids.get(0));
        bookings.createBooking(new CreateBookingRequest("first", null, f.templateId, f.ids.get(1), start, 1, null));
        bookings.createBooking(new CreateBookingRequest("second", null, f.templateId, f.ids.get(1), start.plusHours(2), 1, null));
        assertEquals(1, availability.getAvailableCount(f.templateId, start, start.plusHours(4)));
    }

    @Test void concurrentRentalOfOneInstanceHasExactlyOneWinner() throws Exception {
        var f = fixture(1);
        var results = race(() -> contracts.createContract(f.request(null)));
        assertEquals(1, results.stream().filter(RentalDocumentDto.class::isInstance).count());
        assertEquals(1, results.stream().filter(AppException.class::isInstance).count());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM rental_document_tools WHERE tool_id=?", Integer.class, f.ids.get(0)));
    }

    @Test void duplicateOfflineCreationReturnsSameContractEvenConcurrently() throws Exception {
        var f = fixture(1);
        String offline = UUID.randomUUID().toString();
        var results = race(() -> contracts.createContract(f.request(offline)));
        assertTrue(results.stream().allMatch(RentalDocumentDto.class::isInstance), results.toString());
        assertEquals(((RentalDocumentDto) results.get(0)).id(), ((RentalDocumentDto) results.get(1)).id());
    }

    @Test void closeAndRestoreKeepAllToolsAndRejectRepeatClose() {
        var f = fixture(2);
        var d = contracts.createContract(f.request(null));
        assertEquals(d.updatedAt(), contracts.getById(d.id()).updatedAt());
        contracts.closeContract(d.id(), new CloseContractRequest(100.0, "paid", false, null));
        assertEquals(new HashSet<>(f.ids), contracts.getById(d.id()).toolIds());
        assertEquals(0, assigned(f.ids));
        assertThrows(AppException.class, () -> contracts.closeContract(d.id(), null));
        contracts.restoreContract(d.id());
        assertEquals(2, assigned(f.ids));
        assertEquals(ContractStatus.ACTIVE, contracts.getById(d.id()).status());
    }

    @Test void restoreWithBusySecondToolRollsBackCompletely() {
        var f = fixture(2);
        var old = contracts.createContract(f.request(null));
        contracts.closeContract(old.id(), null);
        contracts.createContract(new CreateContractRequest(f.clientId, f.ids.get(1), null, null, null));
        assertThrows(AppException.class, () -> contracts.restoreContract(old.id()));
        assertEquals(1, assigned(f.ids));
        assertEquals(ContractStatus.CLOSED, contracts.getById(old.id()).status());
    }

    @Test void overlappingBookingsSerializeAndAdjacentBookingIsAllowed() throws Exception {
        var f = fixture(1);
        var start = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0);
        var request = new CreateBookingRequest("Test", null, f.templateId, f.ids.get(0), start, 2, null);
        var results = race(() -> bookings.createBooking(request));
        assertEquals(1, results.stream().filter(BookingDto.class::isInstance).count());
        assertEquals(1, results.stream().filter(AppException.class::isInstance).count());
        assertNotNull(bookings.createBooking(new CreateBookingRequest("Next", null, f.templateId, f.ids.get(0), start.plusHours(2), 1, null)));
    }

    @Test void staleRevisionRollsBackEarlierCreationsAndUpdateThenCloseUsesInitialRevision() {
        var f = fixture(2);
        var original = contracts.createContract(new CreateContractRequest(f.clientId, f.ids.get(0), null, null, null));
        var create = new ContractSyncDto.CreateItem();
        create.setOfflineId(UUID.randomUUID().toString()); create.setClientId(f.clientId); create.setToolId(f.ids.get(1));
        var update = new ContractSyncDto.UpdateItem();
        update.setId(original.id()); update.setComment("update"); update.setExpectedUpdatedAt(original.updatedAt().minusSeconds(1));
        var batch = ContractSyncDto.builder().creations(List.of(create)).updates(List.of(update)).build();
        assertEquals("SYNC_CONFLICT", assertThrows(AppException.class, () -> sync.syncContracts(batch)).getCode());
        assertTrue(documents.findByOfflineId(create.getOfflineId()).isEmpty());
        assertEquals(1, assigned(f.ids));
        update.setExpectedUpdatedAt(original.updatedAt());
        var close = new ContractSyncDto.CloseItem(); close.setId(original.id()); close.setExpectedUpdatedAt(original.updatedAt());
        sync.syncContracts(ContractSyncDto.builder().updates(List.of(update)).closures(List.of(close)).build());
        assertEquals(ContractStatus.CLOSED, contracts.getById(original.id()).status());
    }

    @Test void receiptsReplayAtomicallyConflictOnPayloadAndRollbackWithBusinessOperation() throws Exception {
        var f = fixture(1);
        String key = UUID.randomUUID().toString();
        var request = f.request(null);
        var results = race(() -> asActor("receipt-test", () -> idempotency.execute(key, request,
                RentalDocumentDto.class, () -> contracts.createContract(request))));
        assertTrue(results.stream().allMatch(RentalDocumentDto.class::isInstance), results.toString());
        assertEquals(((RentalDocumentDto)results.get(0)).id(), ((RentalDocumentDto)results.get(1)).id());
        asActor("receipt-test", () -> {
            assertEquals("IDEMPOTENCY_CONFLICT", assertThrows(AppException.class, () -> idempotency.execute(key,
                    f.request("different"), RentalDocumentDto.class, () -> { throw new AssertionError(); })).getCode());
            return null;
        });
        var rollbackFixture = fixture(1);
        String rollbackKey = UUID.randomUUID().toString();
        asActor("receipt-test", () -> {
            assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager).execute(status -> {
                idempotency.execute(rollbackKey, rollbackFixture.request(null), RentalDocumentDto.class,
                        () -> contracts.createContract(rollbackFixture.request(null)));
                throw new IllegalStateException("simulate failure after receipt insert");
            }));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM operation_receipts WHERE operation_id=?", Integer.class, rollbackKey));
            assertEquals(0, assigned(rollbackFixture.ids));
            assertNotNull(idempotency.execute(rollbackKey, rollbackFixture.request(null), RentalDocumentDto.class,
                    () -> contracts.createContract(rollbackFixture.request(null))));
            return null;
        });
    }

    @Test void concurrentRefreshAllowsOneRotationAndReplayRevocationCommits() throws Exception {
        String token = new TransactionTemplate(transactionManager).execute(status -> {
            var user = users.save(User.builder().email(UUID.randomUUID() + "@test.invalid").passwordHash("unused").build());
            var stored = authService.createRefreshForUser(user);
            return jwt.generateRefreshToken(user.getId().toString(), stored.getJti());
        });
        var results = race(() -> auth.refresh(token));
        assertEquals(1, results.stream().filter(AuthResponse.class::isInstance).count());
        assertEquals(1, results.stream().filter(AppException.class::isInstance).count());
        var issued = results.stream().filter(AuthResponse.class::isInstance).map(AuthResponse.class::cast).findFirst().orElseThrow();
        assertThrows(AppException.class, () -> auth.refresh(issued.refreshToken()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE user_id=?", Integer.class, issued.userId()));
    }

    @Test void softDeleteIsAbsentFromFullSnapshotWhileHistoryRemains() {
        var f = fixture(1);
        var d = contracts.createContract(f.request(null));
        contracts.closeContract(d.id(), null);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> documents.deleteById(d.id()));
        var pull = sync.pullSync(java.time.Instant.now(), 1L);
        assertTrue(pull.isFullSnapshot());
        assertTrue(pull.getDocuments().stream().noneMatch(value -> value.id().equals(d.id())));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM rental_document_tools WHERE document_id=?", Integer.class, d.id()));
        assertEquals("CLIENT_HAS_HISTORY", assertThrows(AppException.class, () -> clientService.delete(f.clientId)).getCode());
    }

    private int assigned(List<Long> ids) {
        return (int) ids.stream().filter(id -> jdbc.queryForObject(
                "SELECT contract_id IS NOT NULL FROM tool_instances WHERE id=?", Boolean.class, id)).count();
    }

    private Fixture fixture(int count) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            var client = clients.save(Client.builder().fullName("test-" + UUID.randomUUID()).build());
            var category = categories.save(ToolCategory.builder().name("test-" + UUID.randomUUID()).build());
            var template = templates.save(ToolTemplate.builder().name("test").category(category).dailyRentalPrice(BigDecimal.TEN).build());
            var ids = new ArrayList<Long>();
            for (int i = 0; i < count; i++) ids.add(tools.save(ToolInstance.builder().template(template).build()).getId());
            return new Fixture(client.getId(), template.getId(), ids);
        });
    }

    private record Fixture(Long clientId, UUID templateId, List<Long> ids) {
        CreateContractRequest request(String offline) { return new CreateContractRequest(clientId, null, ids, null, offline); }
    }

    private static <T> T asActor(String actor, Supplier<T> action) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, List.of()));
        try { return action.get(); } finally { SecurityContextHolder.clearContext(); }
    }

    private static List<Object> race(Callable<?> operation) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Object> task = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
            try { return operation.call(); } catch (AppException ex) { return ex; }
        };
        try {
            var a = pool.submit(task); var b = pool.submit(task);
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
}
