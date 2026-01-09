package org.misha.authservice.dto;

/**
 * DTO для адреса.
 */
public record AddressDto(
        String region,
        String street
) {
}
