package org.misha.authservice.service;

import org.junit.jupiter.api.Test;
import org.misha.authservice.dto.CreateContractRequest;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.exception.BadRequestException;
import org.misha.authservice.repository.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContractWriteRegressionTest {
    private final ClientRepository clients = mock(ClientRepository.class);
    private final ToolInstanceRepository tools = mock(ToolInstanceRepository.class);
    private final RentalDocumentRepository documents = mock(RentalDocumentRepository.class);
    private final ContractCrudService service = new ContractCrudService(mock(RentalWriteLock.class), clients,
            tools, documents, mock(ToolRentalGuard.class), mock(AuditLogService.class));

    @Test void invalidCompositionFailsBeforeDatabaseReads() {
        for (List<Long> ids : List.of(List.of(1L, 1L), Arrays.asList(1L, null), List.of(-1L))) {
            assertThrows(BadRequestException.class, () -> service.createContract(
                    new CreateContractRequest(1L, null, ids, null, null)));
        }
        verifyNoInteractions(clients, tools, documents);
    }

    @Test void responseContainsRevisionAfterFlushAndPreservesRequestedPrimaryTool() {
        when(clients.findById(1L)).thenReturn(Optional.of(Client.builder().id(1L).build()));
        when(tools.findAllById(List.of(12L, 11L))).thenReturn(List.of(
                ToolInstance.builder().id(11L).build(), ToolInstance.builder().id(12L).build()));
        var captured = new RentalDocument[1];
        when(documents.save(any())).thenAnswer(call -> {
            captured[0] = call.getArgument(0);
            captured[0].setId(7L);
            return captured[0];
        });
        var revision = LocalDateTime.of(2026, 10, 17, 10, 0);
        doAnswer(call -> { captured[0].setUpdatedAt(revision); return null; }).when(documents).flush();
        var result = service.createContract(new CreateContractRequest(1L, null, List.of(12L, 11L), "R-test", null));
        assertEquals(revision, result.updatedAt());
        assertEquals(12L, result.toolId());
        assertEquals(Set.of(11L, 12L), result.toolIds());
    }
}
