package org.misha.authservice.mapper;

import org.misha.authservice.dto.AddressDto;
import org.misha.authservice.dto.PassportType;
import org.misha.authservice.dto.excel.ClientExcelDto;
import org.misha.authservice.dto.excel.ExcelContractDto;
import org.misha.authservice.dto.excel.RentalExcelDto;
import org.misha.authservice.entity.Address;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ClientPassport;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.ToolInstance;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ExcelContractMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public String buildToolFullName(ToolInstance ToolInstance) {
        if (ToolInstance == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        String name = "";
        if (ToolInstance.getTemplate() != null && ToolInstance.getTemplate().getName() != null) {
            name = ToolInstance.getTemplate().getName();
        }
        if (!name.isBlank()) {
            sb.append(name);
        }

        if (ToolInstance.getInventoryNumber() != null && !ToolInstance.getInventoryNumber().isBlank()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("#").append(ToolInstance.getInventoryNumber());
        }

        return sb.toString();
    }

    public ExcelContractDto toExcelContractDto(
            RentalDocument document,
            ToolInstance ToolInstance,
            Client client) {
        String toolFullName = buildToolFullName(ToolInstance);

        BigDecimal pricePerDay = document.getDailyPrice() != null
                ? BigDecimal.valueOf(document.getDailyPrice())
                : (ToolInstance != null && ToolInstance.getTemplate() != null && ToolInstance.getTemplate().getDailyRentalPrice() != null
                        ? ToolInstance.getTemplate().getDailyRentalPrice()
                        : null);

        BigDecimal depositAmount = ToolInstance != null && ToolInstance.getTemplate() != null && ToolInstance.getTemplate().getDepositAmount() != null
                ? ToolInstance.getTemplate().getDepositAmount()
                : null;

        ClientExcelDto clientDto = toClientExcelDto(client);
        RentalExcelDto rentalDto = toRentalExcelDto(document);

        return new ExcelContractDto(
                toolFullName,
                pricePerDay,
                depositAmount,
                1, // quantity
                clientDto,
                rentalDto);
    }

    private ClientExcelDto toClientExcelDto(Client client) {
        if (client == null) {
            return new ClientExcelDto(
                    "", "", "", "", "", "", "", "", null, null, "", "", null);
        }
        String phone = client.getWhatsappPhone() != null ? client.getWhatsappPhone() : "";
        String whatsapp = client.getAdditionalPhone();
        if (whatsapp != null && whatsapp.equals(phone)) {
            whatsapp = "";
        } else if (whatsapp == null) {
            whatsapp = "";
        }
        ClientPassport passport = client.getPassport();
        String passportType = "";
        String passportNumber = "";
        String passportIssuedBy = "";
        String passportDepartmentCode = "";
        String passportIssuedDate = "";

        if (passport != null) {
            if (passport.getSeries() != null && !passport.getSeries().isBlank()) {
                passportType = PassportType.fromSeries(passport.getSeries()).getCode();
            } else {
                passportType = PassportType.OTHER.getCode();
            }
            if (passport.getNumber() != null && !passport.getNumber().isBlank()) {
                passportNumber = passport.getNumber();
            }
            passportIssuedBy = passport.getIssuedBy() != null ? passport.getIssuedBy() : "";
            passportDepartmentCode = passport.getSubdivisionCode() != null ? passport.getSubdivisionCode() : "";

            if (passport.getIssueDate() != null) {
                passportIssuedDate = passport.getIssueDate().format(DATE_FORMATTER);
            }
        }
        AddressDto registrationAddress = toAddressDto(client.getRegistrationAddress());
        AddressDto livingAddress = toAddressDto(client.getLivingAddress());
        if (livingAddress == null && registrationAddress != null) {
            livingAddress = registrationAddress;
        }
        String pin = client.getPin();
        if (pin == null || pin.isBlank()) {
            if (client.getPassport() != null) {
                pin = client.getPassport().getInn();
            }
        }
        if (pin == null) pin = "";
        String birthDate = "";
        if (client.getBirthDate() != null) {
            birthDate = client.getBirthDate().format(DATE_FORMATTER);
        }

        return new ClientExcelDto(
                client.getFullName() != null ? client.getFullName() : "",
                phone,
                whatsapp,
                passportType,
                passportNumber,
                passportIssuedBy,
                passportDepartmentCode,
                passportIssuedDate,
                registrationAddress,
                livingAddress,
                client.getObjectAddress() != null ? client.getObjectAddress() : "",
                pin,
                birthDate);
    }

    private RentalExcelDto toRentalExcelDto(RentalDocument document) {
        if (document == null) {
            return new RentalExcelDto("", null, null);
        }
        String startDate = "";
        if (document.getStartDateTime() != null) {
            startDate = document.getStartDateTime().toLocalDate().format(DATE_FORMATTER);
        }
        String actualReturnDate = null;
        String actualReturnTime = null;

        if (document.getReturnDate() != null) {
            LocalDateTime returnDate = document.getReturnDate();
            actualReturnDate = returnDate.toLocalDate().format(DATE_FORMATTER);
            actualReturnTime = returnDate.toLocalTime().format(TIME_FORMATTER);
        } else if (document.getTerminatedAt() != null) {
            LocalDateTime terminatedAt = document.getTerminatedAt();
            actualReturnDate = terminatedAt.toLocalDate().format(DATE_FORMATTER);
            actualReturnTime = terminatedAt.toLocalTime().format(TIME_FORMATTER);
        }

        return new RentalExcelDto(startDate, actualReturnDate, actualReturnTime);
    }

    private AddressDto toAddressDto(Address address) {
        if (address == null) return null;
        return new AddressDto(address.getRegion(), address.getStreet());
    }
}
