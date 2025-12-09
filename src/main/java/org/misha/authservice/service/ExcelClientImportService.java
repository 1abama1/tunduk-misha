package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

            CreateClientRequest req = new CreateClientRequest(
                    r.getCell(0).getStringCellValue(), // ФИО
                    r.getCell(1).getStringCellValue(), // телефон
                    null, // whatsapp телефон (в файле нет отдельной колонки)
                    r.getCell(2).getStringCellValue(), // адрес
                    r.getCell(3).getStringCellValue(), // email
                    null, // birthDate
                    "Импорт из Excel",
                    new PassportDto(
                            r.getCell(4).getStringCellValue(), // серия
                            r.getCell(5).getStringCellValue(), // номер
                            r.getCell(6).getStringCellValue(), // кем выдан
                            r.getCell(7).getStringCellValue(), // код
                            null,
                            r.getCell(8).getStringCellValue() // ИНН
                    ),
                    null // tag
            );

            return clientService.create(req);
        }
    }
}


