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
import org.misha.authservice.entity.Tool;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.exception.BadRequestException;
import org.misha.authservice.mapper.ExcelContractMapper;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolRepository;
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
    private final ClientRepository clientRepository;
    private final ToolRepository toolRepository;
    private final ExcelContractMapper excelContractMapper;
    private final ExcelGeneratorService excelGeneratorService;

    private static final String TEMPLATE_CLASSPATH = "templates/lermontov.xlsx";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public byte[] generate(Client client, ContractRequest request) throws IOException {
        Resource template = new ClassPathResource(TEMPLATE_CLASSPATH);
        if (!template.exists()) {
            throw new BadRequestException("Шаблон договора не найден: " + TEMPLATE_CLASSPATH);
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
                        .replace("{{CLIENT_PHONE}}", safe(client.getPhone()))
                        .replace("{{CLIENT_EMAIL}}", safe(client.getEmail()))
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

    @Transactional(readOnly = true)
    public byte[] generateById(Long contractId) {
        RentalDocument document = documentRepository.findById(contractId)
                .orElseThrow(() -> new AppException("CONTRACT_NOT_FOUND", "Договор не найден", HttpStatus.NOT_FOUND));

        Client client = document.getClient();
        if (client == null) {
            throw new AppException("CLIENT_NOT_FOUND", "Клиент не найден для договора", HttpStatus.NOT_FOUND);
        }

        if (client.getPassport() == null) {
            client = clientRepository.findByIdWithDocuments(client.getId()).orElse(client);
        }

        Tool tool = null;
        if (document.getToolId() != null) {
            tool = toolRepository.findByIdWithTemplateAndContract(document.getToolId()).orElse(null);
        }

        if (tool == null) {
            var tools = toolRepository.findByContractIdWithTemplate(contractId);
            if (!tools.isEmpty()) {
                tool = tools.get(0);
            }
        }

        var excelDto = excelContractMapper.toExcelContractDto(document, tool, client);
        return excelGeneratorService.generateContractExcel(excelDto);
    }
}

