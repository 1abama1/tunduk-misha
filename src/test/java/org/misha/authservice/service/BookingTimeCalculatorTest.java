package org.misha.authservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class BookingTimeCalculatorTest {

    @InjectMocks
    private BookingTimeCalculator service;

    @Test
    void testContextLoads() {
        assertNotNull(service);
    }
}
