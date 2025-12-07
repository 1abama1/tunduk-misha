package org.misha.authservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.entity.ToolCategory;
import org.misha.authservice.entity.ToolStatus;
import org.misha.authservice.entity.ToolTemplate;
import org.misha.authservice.repository.ToolCategoryRepository;
import org.misha.authservice.repository.ToolRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ToolDataInitializer {

    private final ToolCategoryRepository categoryRepository;
    private final ToolTemplateRepository templateRepository;
    private final ToolRepository toolRepository;

    public void init() {
        initCategories();
        initTemplates();
        initTools();
    }

    /* ===================== CATEGORIES ===================== */

    private void initCategories() {
        if (categoryRepository.count() > 0) return;

        List<String> names = List.of(
                "Электроинструменты",
                "Ручной инструмент",
                "Садовые инструменты",
                "Строительные",
                "Бензоинструмент",
                "Измерительные приборы",
                "Подъёмное оборудование",
                "Оборудование"
        );

        names.forEach(name ->
                categoryRepository.save(ToolCategory.builder().name(name).build())
        );

        log.info("✅ Categories initialized");
    }

    /* ===================== TEMPLATES ===================== */

    private void initTemplates() {
        if (templateRepository.count() > 0) return;

        List<ToolCategory> categories = categoryRepository.findAll();

        save(find(categories, "Электроинструменты"), List.of(
                "Дрель ударная Bosch GSB 13",
                "Шуруповерт DeWalt DCD776",
                "Перфоратор Makita HR2470"
        ));

        save(find(categories, "Ручной инструмент"), List.of(
                "Молоток слесарный 500 г",
                "Отвертка крестовая PH2",
                "Ножовка по металлу"
        ));

        save(find(categories, "Садовые инструменты"), List.of(
                "Газонокосилка бензиновая Honda",
                "Лопата штыковая"
        ));

        save(find(categories, "Строительные"), List.of(
                "Бетоносмеситель 120 л",
                "Виброплита 90 кг"
        ));

        save(find(categories, "Бензоинструмент"), List.of(
                "Бензопила Stihl MS 180"
        ));

        save(find(categories, "Измерительные приборы"), List.of(
                "Лазерный уровень 360°"
        ));

        save(find(categories, "Подъёмное оборудование"), List.of(
                "Лебедка ручная 1 тонна"
        ));

        save(find(categories, "Оборудование"), List.of(
                "Компрессор 50 л"
        ));

        log.info("✅ Templates initialized");
    }

    private void save(ToolCategory category, List<String> toolNames) {
        toolNames.forEach(name ->
                templateRepository.save(
                        ToolTemplate.builder()
                                .name(name)
                                .category(category)
                                .description(name)
                                .build()
                )
        );
    }

    /* ===================== TOOLS ===================== */

    private void initTools() {
        if (toolRepository.count() > 0) return;

        List<ToolTemplate> templates = templateRepository.findAll();

        templates.forEach(template -> {
            // Создаем по 3 экземпляра для каждой модели
            for (int i = 1; i <= 3; i++) {
                String serial = generateSerial(template.getName(), i);

                Tool tool = Tool.builder()
                        .template(template)
                        .name(template.getName()) // для обратной совместимости
                        .serialNumber(serial)
                        .inventoryNumber(serial) // уникальный инвентарный номер
                        .status(ToolStatus.AVAILABLE)
                        .deposit(1000.0) // базовый залог
                        .createdAt(LocalDateTime.now())
                        .contract(null)
                        .build();

                toolRepository.save(tool);
            }
        });

        log.info("✅ Tools initialized");
    }

    /* ===================== HELPERS ===================== */

    private ToolCategory find(List<ToolCategory> list, String name) {
        return list.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing category: " + name));
    }

    private String generateSerial(String name, int index) {
        return name.substring(0, Math.min(3, name.length()))
                .toUpperCase()
                .replace(" ", "")
                + "-" + index;
    }
}

