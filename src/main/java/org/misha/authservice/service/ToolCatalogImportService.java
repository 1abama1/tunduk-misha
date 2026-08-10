package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.entity.ToolTemplate;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolCatalogImportService {

    private final ToolTemplateRepository templateRepository;
    private final ToolInstanceRepository instanceRepository;

    /**
     * Парсинг прайс-листа и создание шаблонов (ToolTemplate) вместе с заданным количеством экземпляров (ToolInstance).
     */
    @Transactional
    public void importCatalogAndGenerateInstances(String tsvData, int instancesCount) {
        String[] lines = tsvData.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("№")) {
                continue; // Пропуск заголовка или пустой строки
            }

            String[] columns = line.split("\t", -1);
            if (columns.length < 2) continue;

            String legacyId = columns[0].trim();
            String name = columns[1].trim();
            String hourCol = columns.length > 2 ? columns[2].trim() : "";
            String dayCol = columns.length > 3 ? columns[3].trim() : "";
            String dayNightCol = columns.length > 4 ? columns[4].trim() : ""; // Сутки
            String depositCol = columns.length > 5 ? columns[5].trim() : "";
            String priceCol = columns.length > 6 ? columns[6].trim() : "";

            ToolTemplate template = new ToolTemplate();
            template.setName(name);

            Map<String, Object> specs = new HashMap<>();
            specs.put("legacy_id", legacyId);
            
            if (!dayCol.isEmpty()) {
                specs.put("day_price", dayCol);
            }

            // Парсинг "Сутки" -> daily_rental_price
            template.setDailyRentalPrice(parseDecimal(dayNightCol));
            
            // Парсинг "Депозит" -> deposit_amount
            template.setDepositAmount(parseDecimal(depositCol));
            
            // Парсинг "Цена" -> specifications.replacement_cost
            BigDecimal replacementCost = parseDecimal(priceCol);
            if (replacementCost != null) {
                specs.put("replacement_cost", replacementCost);
            }

            // Обработка колонки "Час" (может содержать вес или текстовые условия)
            if (!hourCol.isEmpty()) {
                if (hourCol.matches("\\d+")) {
                    // Если это просто число (например 250, 300)
                    specs.put("weight_category", Integer.parseInt(hourCol));
                } else {
                    specs.put("billing_condition", hourCol);
                }
            }

            template.setSpecifications(specs);
            template = templateRepository.save(template);
            
            // Массовая генерация экземпляров
            generateInstancesForTemplate(template, instancesCount);
        }
    }

    /**
     * Создает нужное количество физических экземпляров (ToolInstance) для шаблона.
     * Нумерация начинается с 001.
     */
    @Transactional
    public List<ToolInstance> generateInstancesForTemplate(ToolTemplate template, int count) {
        // Находим максимальный инвентарный номер, чтобы продолжить счет, если они уже есть
        String lastInventoryNumber = instanceRepository.findMaxInventoryNumberByTemplateId(template.getId());
        int startNumber = 1;
        if (lastInventoryNumber != null && lastInventoryNumber.matches("\\d+")) {
            startNumber = Integer.parseInt(lastInventoryNumber) + 1;
        }

        List<ToolInstance> newInstances = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String invNum = String.format("%03d", startNumber + i);
            ToolInstance instance = new ToolInstance();
            instance.setTemplate(template);
            instance.setInventoryNumber(invNum);
            newInstances.add(instance);
        }
        
        return instanceRepository.saveAll(newInstances);
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        // Убираем " Сом", пробелы (включая неразрывные) и запятые, если они используются как разделители тысяч
        String cleanValue = value.replaceAll("(?i)\\s*Сом\\s*", "")
                                 .replaceAll("\\s+", "") // убираем пробелы (например "2 000,00" -> "2000,00")
                                 .replaceAll(",", ".");  // меняем запятую на точку для BigDecimal
        try {
            return new BigDecimal(cleanValue);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse decimal from string: '{}'", value);
            return null;
        }
    }
}
