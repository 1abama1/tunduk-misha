package org.misha.authservice.service;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.misha.authservice.entity.*;
import org.misha.authservice.mapper.ExcelContractMapper;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelRoundTripTest {
    @Test void everyToolInTenToolContractIsPrinted() throws Exception {
        var tools = java.util.stream.LongStream.rangeClosed(1, 10).mapToObj(id -> ToolInstance.builder().id(id)
                .template(ToolTemplate.builder().name("UniqueTool" + id + "X").build()).build()).toList();
        var dto = new ExcelContractMapper().toExcelContractDto(RentalDocument.builder().build(), tools,
                Client.builder().fullName("Test").build());
        assertEquals(10, dto.tools().size());
        byte[] result = new ExcelGeneratorService().generateContractExcel(dto);
        java.nio.file.Files.write(java.nio.file.Path.of("target", "ten-tool-contract.xlsx"), result);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            assertNotNull(workbook.getSheet("Пр №1 (2)"));
            assertEquals(CellType.STRING, workbook.getSheet("Пр №1 (2)").getRow(19).getCell(1).getCellType());
            var printed = new StringBuilder();
            for (var sheet : workbook) for (var row : sheet) for (var cell : row)
                if (cell.getCellType() == CellType.STRING) printed.append(cell.getStringCellValue()).append('\n');
            for (int i = 1; i <= 10; i++) assertTrue(printed.toString().contains("UniqueTool" + i + "X"), "Missing tool " + i);
        }
    }

    @Test void realTemplateRemainsReadableAndUntrustedTextStaysText() throws Exception {
        var client = Client.builder().fullName("=HYPERLINK(\"https://example.invalid\")").build();
        var tool = ToolInstance.builder().id(1L).inventoryNumber("001")
                .template(ToolTemplate.builder().name("Drill").build()).build();
        var dto = new ExcelContractMapper().toExcelContractDto(RentalDocument.builder().build(), List.of(tool), client);
        byte[] result = new ExcelGeneratorService().generateContractExcel(dto);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            assertNotNull(workbook.getSheet("Дог."));
            var nameCell = workbook.getSheet("Дог.").getRow(17).getCell(0);
            assertEquals(CellType.STRING, nameCell.getCellType());
            assertEquals(client.getFullName(), nameCell.getStringCellValue());
            assertTrue(workbook.getSheet("Пр №1").getRow(19).getCell(1).getStringCellValue().contains("Drill"));
        }
    }
}
