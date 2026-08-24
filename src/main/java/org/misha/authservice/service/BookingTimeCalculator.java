package org.misha.authservice.service;

import org.misha.authservice.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class BookingTimeCalculator {

    private static final LocalTime OPENING_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(18, 0);

    public LocalDateTime calculateEndDateTime(LocalDateTime startDateTime, int hours) {
        if (hours < 1 || hours > 6) {
            throw new AppException("INVALID_HOURS", "Booking hours must be between 1 and 6", HttpStatus.BAD_REQUEST);
        }

        LocalTime startTime = startDateTime.toLocalTime();
        
        // If booking starts before opening time, we snap it to opening time
        if (startTime.isBefore(OPENING_TIME)) {
            startDateTime = startDateTime.with(OPENING_TIME);
            startTime = OPENING_TIME;
        }
        
        // If booking starts after closing time, we snap it to opening time of the NEXT day
        if (startTime.isAfter(CLOSING_TIME) || startTime.equals(CLOSING_TIME)) {
            startDateTime = startDateTime.plusDays(1).with(OPENING_TIME);
            startTime = OPENING_TIME;
        }

        LocalDateTime endDateTime = startDateTime.plusHours(hours);
        LocalTime endTime = endDateTime.toLocalTime();

        // If the resulting end time is after closing time (or goes to next day), carry over to the next day
        if (endTime.isAfter(CLOSING_TIME) || endDateTime.toLocalDate().isAfter(startDateTime.toLocalDate())) {
            // Calculate exactly how many minutes we consumed today
            long minutesConsumedToday = java.time.Duration.between(startTime, CLOSING_TIME).toMinutes();
            long totalMinutes = hours * 60L;
            long remainingMinutes = totalMinutes - minutesConsumedToday;
            
            // Carry over remaining time to the next day starting at OPENING_TIME
            endDateTime = startDateTime.plusDays(1).with(OPENING_TIME).plusMinutes(remainingMinutes);
        }

        return endDateTime;
    }
}
