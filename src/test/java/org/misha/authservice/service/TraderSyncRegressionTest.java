package org.misha.authservice.service;

import org.junit.jupiter.api.Test;
import org.misha.authservice.entity.Trader;
import org.misha.authservice.exception.BadRequestException;
import org.misha.authservice.repository.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TraderSyncRegressionTest {
    @Test void invalidTimestampDoesNotSilentlyReturnFullDataset() {
        var repository = mock(ProductRepository.class);
        assertThrows(BadRequestException.class, () -> new ProductSyncService(repository).syncProducts(new Trader(), "invalid", null, 0, 20));
        verifyNoInteractions(repository);
    }

    @Test void databaseFailureIsNotRetriedAsFullSync() {
        var repository = mock(OrderRepository.class);
        when(repository.findByTraderAndUpdatedAfter(any(), any(), any())).thenThrow(new DataAccessResourceFailureException("offline"));
        assertThrows(DataAccessResourceFailureException.class, () -> new OrderSyncService(repository)
                .syncOrders(new Trader(), "2026-10-17T00:00:00Z", null, 0, 20));
        verify(repository, never()).findByTrader(any(), any());
    }

    @Test void pageSizeIsBoundedAndOrderingIsStable() {
        var repository = mock(ProductRepository.class);
        new ProductSyncService(repository).syncProducts(new Trader(), null, null, 0, Integer.MAX_VALUE);
        var argument = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByTrader(any(), argument.capture());
        assertEquals(200, argument.getValue().getPageSize());
        assertNotNull(argument.getValue().getSort().getOrderFor("id"));
    }
}
