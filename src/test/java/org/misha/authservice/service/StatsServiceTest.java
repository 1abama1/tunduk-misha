package org.misha.authservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @InjectMocks
    private StatsService service;

    @Test
    void testContextLoads() {
        assertNotNull(service);
    }

    @Test
    void countsToolsInRepair() {
        var contracts = org.mockito.Mockito.mock(org.misha.authservice.repository.RentalDocumentRepository.class);
        var tools = org.mockito.Mockito.mock(org.misha.authservice.repository.ToolInstanceRepository.class);
        org.mockito.Mockito.when(tools.countByStatus(org.misha.authservice.entity.ToolInstanceStatus.IN_REPAIR)).thenReturn(3L);
        org.junit.jupiter.api.Assertions.assertEquals(3L, new StatsService(contracts, tools).getSummary().brokenTools());
    }
}
