package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.misha.authservice.dto.AddressDto;
import org.misha.authservice.dto.ClientDto;
import org.misha.authservice.dto.CreateClientRequest;
import org.misha.authservice.dto.PassportDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ExcelClientImportService {

    private final ClientService clientService;
    private final DataFormatter dataFormatter = new DataFormatter();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public ClientDto importClient(MultipartFile file) throws IOException {
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            // ФИО
            String fullName = getValue(sheet, "A18");
            // Телефон
            String phone = getValue(sheet, "D7");
            // WhatsApp (если есть)
            String whatsapp = getValue(sheet, "G7");

            // ИНН (C22)
            String inn = getValue(sheet, "C22");

            // Паспортные данные
            String passportSeries = getValue(sheet, "E19");
            String passportNumber = getValue(sheet, "G19");
            String issuedBy = getValue(sheet, "J19");
            String subdivisionCode = getValue(sheet, "K19");

            // Адрес регистрации (Регион в C20, Улица в J20)
            String regRegion = getValue(sheet, "C20");
            String regStreet = getValue(sheet, "J20");
            AddressDto registrationAddress = (regRegion != null || regStreet != null)
                    ? new AddressDto(regRegion, regStreet)
                    : null;

            // Фактический адрес (Регион в A21, Улица в I21)
            String liveRegion = getValue(sheet, "A21");
            String liveStreet = getValue(sheet, "I21");
            AddressDto livingAddress = (liveRegion != null || liveStreet != null)
                    ? new AddressDto(liveRegion, liveStreet)
                    : null;

            // Адрес объекта (K22)
            String objectAddress = getValue(sheet, "K22");

            // Дата рождения (A23)
            String birthDateStr = getValue(sheet, "A23");
            LocalDate birthDate = parseDate(birthDateStr);

            CreateClientRequest req = new CreateClientRequest(
                    fullName,
                    phone,
                    whatsapp,
                    registrationAddress,
                    livingAddress,
                    objectAddress,
                    null, // email
                    birthDate,
                    "Импорт из Excel (шаблон)", // comment
                    new PassportDto(
                            passportSeries,
                            passportNumber,
                            issuedBy,
                            subdivisionCode,
                            null, // issueDate
                            inn),
                    null // tag
            );

            return clientService.create(req);
        }
    }

    private String getValue(Sheet sheet, String cellRef) {
        try {
            int rowIdx = parseRowIndex(cellRef);
            int colIdx = parseColumnIndex(cellRef);
            var row = sheet.getRow(rowIdx);
            if (row == null)
                return null;
            var cell = row.getCell(colIdx);
            if (cell == null)
                return null;
            return dataFormatter.formatCellValue(cell);
        } catch (Exception e) {
            return null;
        }
    }

    private int parseRowIndex(String cellRef) {
        String rowStr = cellRef.replaceAll("[^0-9]", "");
        return Integer.parseInt(rowStr) - 1;
    }

    private int parseColumnIndex(String cellRef) {
        String colStr = cellRef.replaceAll("[^A-Za-z]", "").toUpperCase();
        int colIdx = 0;
        for (int i = 0; i < colStr.length(); i++) {
            colIdx = colIdx * 26 + (colStr.charAt(i) - 'A' + 1);
        }
        return colIdx - 1;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}
