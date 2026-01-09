package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.misha.authservice.dto.AddressDto;
import org.misha.authservice.dto.ClientDto;
import org.misha.authservice.dto.CreateClientRequest;
import org.misha.authservice.dto.PassportDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ExcelClientImportService {

    private final ClientService clientService;

    public ClientDto importClient(MultipartFile file) throws IOException {
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Row r = sheet.getRow(1); // первая строка с данными

            // Парсим адрес регистрации из Excel (весь адрес в region, street = null)
            String regAddressStr = r.getCell(2).getStringCellValue();
            AddressDto registrationAddress = regAddressStr != null && !regAddressStr.isBlank()
                    ? new AddressDto(regAddressStr, null)
                    : null;

            CreateClientRequest req = new CreateClientRequest(
                    r.getCell(0).getStringCellValue(), // fullName
                    r.getCell(1).getStringCellValue(), // phone
                    null, // whatsappPhone (в файле нет отдельной колонки)
                    registrationAddress,
                    null, // livingAddress (в файле нет отдельной колонки)
                    r.getCell(3).getStringCellValue(), // email
                    null, // birthDate
                    "Импорт из Excel", // comment
                    new PassportDto(
                            r.getCell(4).getStringCellValue(), // series
                            r.getCell(5).getStringCellValue(), // number
                            r.getCell(6).getStringCellValue(), // issuedBy
                            r.getCell(7).getStringCellValue(), // subdivisionCode
                            null, // issueDate
                            r.getCell(8).getStringCellValue() // inn
                    ),
                    null // tag
            );

            return clientService.create(req);
        }
    }
}


