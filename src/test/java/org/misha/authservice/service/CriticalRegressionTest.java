package org.misha.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.misha.authservice.dto.*;
import org.misha.authservice.entity.*;
import org.misha.authservice.exception.*;
import org.misha.authservice.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CriticalRegressionTest {
    @Test void closingBrokenMultiToolContractPreservesEveryTool() {
        var docs = mock(RentalDocumentRepository.class);
        var tools = mock(ToolInstanceRepository.class);
        var lock = mock(RentalWriteLock.class);
        var doc = RentalDocument.builder().id(7L).contractNumber("R-7")
            .startDateTime(LocalDateTime.of(2026, 1, 1, 10, 0)).build();
        var first = ToolInstance.builder().id(11L).contract(doc).status(ToolInstanceStatus.AVAILABLE).build();
        var second = ToolInstance.builder().id(12L).contract(doc).status(ToolInstanceStatus.AVAILABLE).build();
        when(docs.findById(7L)).thenReturn(Optional.of(doc));
        when(tools.findByContractId(7L)).thenReturn(List.of(first, second));
        var service = new ContractCrudService(lock, mock(ClientRepository.class), tools, docs,
            mock(ToolRentalGuard.class), mock(AuditLogService.class));
        service.closeContract(7L, new CloseContractRequest(100.0, "повреждение", true, doc.getStartDateTime().plusDays(1)));
        assertEquals(Set.of(11L,12L), doc.getHistoricalToolIds());
        assertNull(first.getContract()); assertNull(second.getContract());
        assertEquals(ToolInstanceStatus.IN_REPAIR, first.getStatus());
        assertEquals(ToolInstanceStatus.IN_REPAIR, second.getStatus());
        verify(lock).acquire(); verify(docs).save(doc);
    }

    @Test void invalidPaymentsAndDatesFailBeforeClosing() {
        var doc = RentalDocument.builder().startDateTime(LocalDateTime.of(2026,1,1,10,0)).build();
        for (double amount : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(BadRequestException.class, () -> RentalValidation.close(doc, new CloseContractRequest(amount,null,false,null)));
        assertThrows(BadRequestException.class, () -> RentalValidation.close(doc,
            new CloseContractRequest(0.0,null,false,doc.getStartDateTime().minusSeconds(1))));
        assertDoesNotThrow(() -> RentalValidation.close(doc,new CloseContractRequest(0.0,null,false,doc.getStartDateTime())));
    }

    @Test void bothDebtorRepresentationsAreRejected() {
        assertThrows(BadRequestException.class, () -> RentalValidation.client(Client.builder().tag(Tag.Должник).build()));
        assertThrows(BadRequestException.class, () -> RentalValidation.client(Client.builder().tags(Set.of(ClientTag.DEBTOR)).build()));
    }

    @Test void repairAndBookingCannotBeRented() {
        var docs = mock(RentalDocumentRepository.class);
        var bookings = mock(ToolBookingRepository.class);
        var guard = new ToolRentalGuard(docs, bookings);
        var tool = ToolInstance.builder().id(1L).status(ToolInstanceStatus.IN_REPAIR).build();
        when(bookings.findByToolInstanceId(1L)).thenReturn(List.of());
        assertThrows(AppException.class, () -> guard.ensureAvailableForRental(tool));
        tool.setStatus(ToolInstanceStatus.AVAILABLE);
        when(bookings.findByToolInstanceId(1L)).thenReturn(List.of(ToolBooking.builder()
            .status(BookingStatus.ACTIVE).endDateTime(LocalDateTime.now().plusDays(1)).build()));
        assertThrows(AppException.class, () -> guard.ensureAvailableForRental(tool));
    }

    @Test void syncResponseCanBeReplayedFromReceipt() throws Exception {
        var mapper = new ObjectMapper().findAndRegisterModules();
        var value = ContractSyncDto.SyncResponse.builder().idMappings(List.of(new ContractSyncDto.IdMapping("offline",9L,"R-9"))).build();
        var restored = mapper.readValue(mapper.writeValueAsString(value), ContractSyncDto.SyncResponse.class);
        assertEquals(9L, restored.getIdMappings().get(0).getBackendId());
    }

    @Test void secretFieldsAreNeverSerialized() throws Exception {
        var user = new User(); user.setPasswordHash("secret-password");
        var trader = new Trader(); trader.setApiKey("secret-api-key"); trader.setUser(user);
        var product = new Product(); product.setTrader(trader);
        var mapper = new ObjectMapper().findAndRegisterModules();
        assertFalse(mapper.writeValueAsString(product).contains("secret-"));
        assertFalse(mapper.writeValueAsString(trader).contains("secret-"));
        assertFalse(mapper.writeValueAsString(user).contains("secret-password"));
    }
}
