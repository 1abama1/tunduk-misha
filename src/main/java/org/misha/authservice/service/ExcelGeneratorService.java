package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.misha.authservice.dto.excel.ExcelContractDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;

/**
 * Сервис для генерации Excel договоров и актов по шаблону.
 * 
 * Правила:
 * - Все значения подготавливаются на бэке
 * - Excel получает готовые строки, числа, даты
 * - Excel не собирает данные из нескольких полей
 * - Формулы допустимы только для визуальных расчётов
 * - Backend не трогает ручные поля (заполняемые офисом)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelGeneratorService {

    private static final String TEMPLATE_PATH = "templates/lermontov.xlsx";

    /**
     * Генерирует Excel файл договора по шаблону.
     * 
     * @param dto данные для заполнения
     * @return массив байтов готового .xlsx файла
     */
    public byte[] generateContractExcel(ExcelContractDto dto) {
        try (
                InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
                Workbook workbook = new XSSFWorkbook(is);
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            fillContractSheet(workbook, dto);
            fillToolPriceSheet(workbook, dto);
            fillActSheet(workbook, dto);

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Ошибка генерации Excel договора", e);
            throw new RuntimeException("Ошибка генерации Excel договора: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------
    // Лист: "Дог."
    // -------------------------------------------------------

    private void fillContractSheet(Workbook workbook, ExcelContractDto dto) {
        Sheet sheet = workbook.getSheet("Дог.");
        if (sheet == null) {
            log.warn("Лист 'Дог.' не найден в шаблоне");
            return;
        }

        // Инструмент
        set(sheet, "A12", dto.toolFullName());

        // Контакты
        set(sheet, "D7", dto.client().phone());
        set(sheet, "G7", dto.client().whatsapp());

        // ФИО
        set(sheet, "A18", dto.client().fullName());

        // Паспорт
        set(sheet, "E19", dto.client().passportType());
        set(sheet, "G19", dto.client().passportNumber());
        set(sheet, "J19", dto.client().passportIssuedBy());
        set(sheet, "K19", dto.client().passportDepartmentCode());
        set(sheet, "M19", dto.client().passportIssuedDate());

        // Адреса и персональные данные
        // Регистрация: регион → C20, улица → J20
        if (dto.client().registrationAddress() != null) {
            set(sheet, "C20", dto.client().registrationAddress().region());
            set(sheet, "J20", dto.client().registrationAddress().street());
        }
        // Фактический: регион → A21, улица → I21
        if (dto.client().livingAddress() != null) {
            set(sheet, "A21", dto.client().livingAddress().region());
            set(sheet, "I21", dto.client().livingAddress().street());
        }
        set(sheet, "C22", dto.client().pin());
        if (dto.client().birthYear() != null) {
            set(sheet, "A23", dto.client().birthYear().toString());
        }

        // K22 — НЕ ТРОГАЕМ (ручное поле)
    }

    // -------------------------------------------------------
    // Лист: "Пр №1"
    // -------------------------------------------------------

    private void fillToolPriceSheet(Workbook workbook, ExcelContractDto dto) {
        Sheet sheet = workbook.getSheet("Пр №1");
        if (sheet == null) {
            log.warn("Лист 'Пр №1' не найден в шаблоне");
            return;
        }

        if (dto.pricePerDay() != null) {
            set(sheet, "I20", dto.pricePerDay());
        }
        if (dto.depositAmount() != null) {
            set(sheet, "P20", dto.depositAmount());
        }
    }

    // -------------------------------------------------------
    // Лист: "Акт расч."
    // -------------------------------------------------------

    private void fillActSheet(Workbook workbook, ExcelContractDto dto) {
        Sheet sheet = workbook.getSheet("Акт расч.");
        if (sheet == null) {
            log.warn("Лист 'Акт расч.' не найден в шаблоне");
            return;
        }

        if (dto.rental().actualReturnDate() != null) {
            set(sheet, "G21", dto.rental().actualReturnDate());
        }

        if (dto.rental().actualReturnTime() != null) {
            set(sheet, "G20", dto.rental().actualReturnTime());
        }

        // Очистка ячеек
        clear(sheet, "H20");
        clear(sheet, "H21");

        // Формула расчёта L21 = G21 - F21
        setFormula(sheet, "L21", "G21-F21");
    }

    // -------------------------------------------------------
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // -------------------------------------------------------

    /**
     * Устанавливает значение в ячейку.
     * Поддерживает Number и String.
     */
    private void set(Sheet sheet, String cellRef, Object value) {
        if (value == null) {
            return;
        }

        Cell cell = getCell(sheet, cellRef);

        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof BigDecimal bigDecimal) {
            cell.setCellValue(bigDecimal.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * Очищает ячейку.
     */
    private void clear(Sheet sheet, String cellRef) {
        Cell cell = getCell(sheet, cellRef);
        cell.setBlank();
    }

    /**
     * Устанавливает формулу в ячейку.
     */
    private void setFormula(Sheet sheet, String cellRef, String formula) {
        if (formula == null || formula.isBlank()) {
            return;
        }
        Cell cell = getCell(sheet, cellRef);
        cell.setCellFormula(formula);
    }

    /**
     * Получает или создаёт ячейку по адресу (например, "A12").
     * 
     * @param sheet лист
     * @param cellRef адрес ячейки в формате Excel (например, "A12", "AB5")
     * @return ячейка
     */
    private Cell getCell(Sheet sheet, String cellRef) {
        // Парсим адрес ячейки (например, "A12" -> row=11, col=0)
        int rowIdx = parseRowIndex(cellRef);
        int colIdx = parseColumnIndex(cellRef);

        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }

        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }

        return cell;
    }

    /**
     * Парсит индекс строки из адреса ячейки (например, "A12" -> 11).
     * Excel использует 1-based индексы, POI - 0-based.
     */
    private int parseRowIndex(String cellRef) {
        // Убираем все буквы, оставляем только цифры
        String rowStr = cellRef.replaceAll("[^0-9]", "");
        if (rowStr.isEmpty()) {
            throw new IllegalArgumentException("Неверный формат адреса ячейки: " + cellRef);
        }
        // Excel использует 1-based, POI - 0-based
        return Integer.parseInt(rowStr) - 1;
    }

    /**
     * Парсит индекс колонки из адреса ячейки (например, "A12" -> 0, "AB5" -> 27).
     */
    private int parseColumnIndex(String cellRef) {
        // Убираем все цифры, оставляем только буквы
        String colStr = cellRef.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (colStr.isEmpty()) {
            throw new IllegalArgumentException("Неверный формат адреса ячейки: " + cellRef);
        }

        int colIdx = 0;
        for (int i = 0; i < colStr.length(); i++) {
            char ch = colStr.charAt(i);
            colIdx = colIdx * 26 + (ch - 'A' + 1);
        }
        // Excel использует 1-based, POI - 0-based
        return colIdx - 1;
    }
}

