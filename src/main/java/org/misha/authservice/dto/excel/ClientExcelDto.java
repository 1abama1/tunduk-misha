package org.misha.authservice.dto.excel;

import org.misha.authservice.dto.AddressDto;

/**
 * DTO для данных клиента в Excel.
 */
public record ClientExcelDto(
        String fullName,
        String whatsappPhone,
        String additionalPhone, // Если совпадает с whatsappPhone, то пустая строка
        String passportType, // ID, AN, MIA и т.д.
        String passportNumber,
        String passportIssuedBy,
        String passportDepartmentCode,
        String passportIssuedDate, // Форматированная дата (dd.MM.yyyy)
        AddressDto registrationAddress,
        AddressDto livingAddress,
        String objectAddress,
        String pin,
        String birthDate) {
}
