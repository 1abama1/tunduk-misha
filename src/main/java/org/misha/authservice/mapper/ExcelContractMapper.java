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
import org.misha.authservice.entity.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mapper для преобразования сущностей в DTO для генерации Excel.
 */
@Component
public class ExcelContractMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Формирует полное имя инструмента в формате: <название> <модель> #<номер>
     * 
     * Пример: "Отбойник BOSCH 16-30 #1"
     */
    public String buildToolFullName(Tool tool) {
        if (tool == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Название (из template или tool.name)
        String name = tool.getName();
        if (name == null || name.isBlank()) {
            if (tool.getTemplate() != null && tool.getTemplate().getName() != null) {
                name = tool.getTemplate().getName();
            } else {
                name = "";
            }
        }
        if (!name.isBlank()) {
            sb.append(name);
        }

        // Модель (article или serialNumber)
        String model = tool.getArticle();
        if (model == null || model.isBlank()) {
            model = tool.getSerialNumber();
        }
        if (model != null && !model.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(model);
        }

        // Номер экземпляра
        if (tool.getInstanceNumber() != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("#").append(tool.getInstanceNumber());
        } else if (tool.getInventoryNumber() != null && !tool.getInventoryNumber().isBlank()) {
            // Если нет instanceNumber, используем inventoryNumber
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("#").append(tool.getInventoryNumber());
        }

        return sb.toString();
    }

    /**
     * Преобразует сущности в ExcelContractDto.
     */
    public ExcelContractDto toExcelContractDto(
            RentalDocument document,
            Tool tool,
            Client client
    ) {
        // Формируем toolFullName
        String toolFullName = buildToolFullName(tool);

        // Цены
        BigDecimal pricePerDay = document.getDailyPrice() != null
                ? BigDecimal.valueOf(document.getDailyPrice())
                : (tool != null && tool.getDailyPrice() != null
                        ? BigDecimal.valueOf(tool.getDailyPrice())
                        : null);

        BigDecimal depositAmount = tool != null && tool.getDeposit() != null
                ? BigDecimal.valueOf(tool.getDeposit())
                : null;

        // Данные клиента
        ClientExcelDto clientDto = toClientExcelDto(client);

        // Данные аренды
        RentalExcelDto rentalDto = toRentalExcelDto(document);

        return new ExcelContractDto(
                toolFullName,
                pricePerDay,
                depositAmount,
                clientDto,
                rentalDto
        );
    }

    /**
     * Преобразует Client в ClientExcelDto.
     */
    private ClientExcelDto toClientExcelDto(Client client) {
        if (client == null) {
            return new ClientExcelDto(
                    "", "", "", "", "", "", "", "", null, null, "", null
            );
        }

        // Телефон
        String phone = client.getPhone() != null ? client.getPhone() : "";

        // WhatsApp (если совпадает с phone, то пустая строка)
        String whatsapp = client.getWhatsappPhone();
        if (whatsapp != null && whatsapp.equals(phone)) {
            whatsapp = "";
        } else if (whatsapp == null) {
            whatsapp = "";
        }

        // Паспорт
        ClientPassport passport = client.getPassport();
        String passportType = "";
        String passportNumber = "";
        String passportIssuedBy = "";
        String passportDepartmentCode = "";
        String passportIssuedDate = "";

        if (passport != null) {
            // Определяем тип паспорта
            if (passport.getSeries() != null && !passport.getSeries().isBlank()) {
                passportType = PassportType.fromSeries(passport.getSeries()).getCode();
            } else {
                passportType = PassportType.OTHER.getCode();
            }

            // Номер паспорта (только number, без серии - для Excel G19)
            if (passport.getNumber() != null && !passport.getNumber().isBlank()) {
                passportNumber = passport.getNumber();
            }

            passportIssuedBy = passport.getIssuedBy() != null ? passport.getIssuedBy() : "";
            passportDepartmentCode = passport.getSubdivisionCode() != null ? passport.getSubdivisionCode() : "";
            
            if (passport.getIssueDate() != null) {
                passportIssuedDate = passport.getIssueDate().format(DATE_FORMATTER);
            }
        }

        // Адреса
        AddressDto registrationAddress = toAddressDto(client.getRegistrationAddress());
        AddressDto livingAddress = toAddressDto(client.getLivingAddress());
        // Если фактический адрес не указан, используем адрес регистрации
        if (livingAddress == null && registrationAddress != null) {
            livingAddress = registrationAddress;
        }

        // PIN и год рождения
        String pin = client.getPin() != null ? client.getPin() : "";
        Integer birthYear = client.getBirthYear();
        if (birthYear == null && client.getBirthDate() != null) {
            birthYear = client.getBirthDate().getYear();
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
                pin,
                birthYear
        );
    }

    /**
     * Преобразует RentalDocument в RentalExcelDto.
     */
    private RentalExcelDto toRentalExcelDto(RentalDocument document) {
        if (document == null) {
            return new RentalExcelDto("", null, null);
        }

        // Дата начала
        String startDate = "";
        if (document.getStartDateTime() != null) {
            startDate = document.getStartDateTime().toLocalDate().format(DATE_FORMATTER);
        }

        // Дата и время возврата
        String actualReturnDate = null;
        String actualReturnTime = null;

        if (document.getClosedAt() != null) {
            LocalDateTime closedAt = document.getClosedAt();
            actualReturnDate = closedAt.toLocalDate().format(DATE_FORMATTER);
            actualReturnTime = closedAt.toLocalTime().format(TIME_FORMATTER);
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

