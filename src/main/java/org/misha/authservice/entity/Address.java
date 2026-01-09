package org.misha.authservice.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value Object для адреса.
 * Содержит две части: регион/город/село и улица/дом/квартира.
 */
@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    /**
     * Область / город / село
     */
    private String region;

    /**
     * Улица / дом / квартира
     */
    private String street;
}
