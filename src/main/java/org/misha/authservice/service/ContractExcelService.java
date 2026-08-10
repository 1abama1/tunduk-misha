package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.misha.authservice.dto.ContractRequest;
import org.misha.authservice.dto.excel.ExcelContractDto;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.exception.BadRequestException;
import org.misha.authservice.mapper.ExcelContractMapper;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ContractExcelService {

    private final RentalDocumentRepository documentRepository;
    private final ToolInstanceRepository ToolInstanceRepository;
    private final ExcelContractMapper excelContractMapper;
    private final ExcelGeneratorService excelGeneratorService;

    private static final String TEMPLATE_CLASSPATH = "templates/lermontov.xlsx";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public byte[] generate(Client client, ContractRequest request) throws IOException {
        Resource template = new ClassPathResource(TEMPLATE_CLASSPATH);
        if (!template.exists()) {
            throw new BadRequestException("РЁР°Р±Р»РѕРЅ РґРѕРіРѕРІРѕСЂР° РЅРµ РЅР°Р№РґРµРЅ: " + TEMPLATE_CLASSPATH);
        }

        try (InputStream is = template.getInputStream();
             Workbook workbook = new XSSFWorkbook(is);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                replacePlaceholdersInSheet(sheet, client, request);
            }

            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private void replacePlaceholdersInSheet(Sheet sheet, Client client, ContractRequest req) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() != CellType.STRING) {
                    continue;
                }
                String original = cell.getStringCellValue();
                String updated = original
                        .replace("{{CLIENT_FULLNAME}}", safe(client.getFullName()))
                        .replace("{{CLIENT_PHONE}}", safe(client.getWhatsappPhone()))
                        .replace("{{CLIENT_ADDRESS}}", safe(
                                client.getRegistrationAddress() != null
                                        ? client.getRegistrationAddress()
                                        : client.getLivingAddress()))
                        .replace("{{CONTRACT_NUMBER}}", safe(req.getContractNumber()))
                        .replace("{{PRICE}}", formatPrice(req.getPrice()))
                        .replace("{{RENT_PRICE}}", formatPrice(req.getPrice()))
                        .replace("{{TOOL_NAME}}", "")
                        .replace("{{TOOL_SERIAL}}", "")
                        .replace("{{TOOL_SN}}", "")
                        .replace("{{DATE}}", DATE_FORMATTER.format(OffsetDateTime.now()));

                if (!original.equals(updated)) {
                    cell.setCellValue(updated);
                }
            }
        }
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private String formatPrice(Double price) {
        if (price == null) {
            return "";
        }
        return String.format(Locale.US, "%.2f", price);
    }

    /**
     * Р“РµРЅРµСЂРёСЂСѓРµС‚ Excel РїРѕ ID РґРѕРіРѕРІРѕСЂР°.
     *
     * РћРїС‚РёРјРёР·РёСЂРѕРІР°РЅ: РєР»РёРµРЅС‚ + РїР°СЃРїРѕСЂС‚ Р·Р°РіСЂСѓР¶Р°СЋС‚СЃСЏ РѕРґРЅРёРј JOIN FETCH Р·Р°РїСЂРѕСЃРѕРј
     * С‡РµСЂРµР· {@code findByIdForExcel}, Р±РµР· РґРѕРїРѕР»РЅРёС‚РµР»СЊРЅС‹С… lazy-РѕР±СЂР°С‰РµРЅРёР№.
     * ToolInstance РёС‰РµС‚СЃСЏ РѕС‚РґРµР»СЊРЅРѕ (РїРѕ toolId РёР»Рё РїРѕ contractId РєР°Рє fallback).
     */
    @Transactional(readOnly = true)
    public byte[] generateById(Long contractId) {
        // РћРґРёРЅ Р·Р°РїСЂРѕСЃ: document + client + passport (LEFT JOIN FETCH)
        RentalDocument document = documentRepository.findByIdForExcel(contractId)
                .orElseThrow(() -> new AppException("CONTRACT_NOT_FOUND", "Р”РѕРіРѕРІРѕСЂ РЅРµ РЅР°Р№РґРµРЅ", HttpStatus.NOT_FOUND));

        Client client = document.getClient();
        if (client == null) {
            throw new AppException("CLIENT_NOT_FOUND", "РљР»РёРµРЅС‚ РЅРµ РЅР°Р№РґРµРЅ РґР»СЏ РґРѕРіРѕРІРѕСЂР°", HttpStatus.NOT_FOUND);
        }

        // РС‰РµРј РёРЅСЃС‚СЂСѓРјРµРЅС‚: СЃРЅР°С‡Р°Р»Р° РїРѕ toolId, Р·Р°С‚РµРј fallback РїРѕ contractId
        ToolInstance ToolInstance = null;
        if (document.getToolId() != null) {
            ToolInstance = ToolInstanceRepository.findByIdWithTemplateAndContract(document.getToolId()).orElse(null);
        }
        if (ToolInstance == null) {
            var tools = ToolInstanceRepository.findByContractIdWithTemplate(contractId);
            if (!tools.isEmpty()) {
                ToolInstance = tools.get(0);
            }
        }

        var excelDto = excelContractMapper.toExcelContractDto(document, ToolInstance, client);
        return excelGeneratorService.generateContractExcel(excelDto);
    }
}


