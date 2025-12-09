package org.misha.authservice.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.misha.authservice.entity.Branch;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.entity.ToolCategory;
import org.misha.authservice.entity.ToolStatus;
import org.misha.authservice.entity.ToolTemplate;
import org.misha.authservice.repository.BranchRepository;
import org.misha.authservice.repository.ToolCategoryRepository;
import org.misha.authservice.repository.ToolRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ToolDataInitializer {

    private final BranchRepository branchRepository;
    private final ToolCategoryRepository categoryRepository;
    private final ToolTemplateRepository templateRepository;
    private final ToolRepository toolRepository;

    @Transactional
    public void init() {
        initBranches();
        initCategoriesTemplatesTools();
        log.info("🎉 ToolDataInitializer finished successfully");
    }

    private void initBranches() {
        if (branchRepository.count() > 0) {
            return;
        }
        branchRepository.saveAll(List.of(
                Branch.builder().name("Филиал Бишкек — Восток").build(),
                Branch.builder().name("Филиал Бишкек — Юг").build(),
                Branch.builder().name("Филиал Ош — Центр").build()
        ));
        log.info("✅ Branches initialized");
    }

    private void initCategoriesTemplatesTools() {
        if (categoryRepository.count() > 0) {
            log.info("Categories already initialized — skipped");
            return;
        }

        Branch defaultBranch = branchRepository.findAll().stream().findFirst().orElse(null);

        ToolCategory perforators = categoryRepository.save(ToolCategory.builder().name("Перфораторы").build());
        ToolCategory drills = categoryRepository.save(ToolCategory.builder().name("Дрели").build());
        ToolCategory grinders = categoryRepository.save(ToolCategory.builder().name("Болгарки").build());

        ToolTemplate bosch820 = templateRepository.save(
                ToolTemplate.builder().category(perforators).name("Bosch 820 L").build()
        );

        ToolTemplate makita243 = templateRepository.save(
                ToolTemplate.builder().category(drills).name("Makita 243D").build()
        );

        ToolTemplate dewalt991 = templateRepository.save(
                ToolTemplate.builder().category(grinders).name("DeWalt 991X").build()
        );

        createTools(bosch820, 3, 500, 3000, defaultBranch);
        createTools(makita243, 4, 450, 2500, defaultBranch);
        createTools(dewalt991, 2, 600, 3500, defaultBranch);

        log.info("✅ Categories, templates and tools created");
    }

    private void createTools(ToolTemplate template, int count, double dailyPrice, double deposit, Branch branch) {
        for (int i = 1; i <= count; i++) {
            Tool tool = Tool.builder()
                    .template(template)
                    .name(template.getName() + " №" + i)
                    .instanceNumber(i)
                    .inventoryNumber(generateInventory(template, i))
                    .article(generateArticle(template, i))
                    .deposit(deposit)
                    .dailyPrice(dailyPrice)
                    .purchasePrice(deposit * 1.5)
                    .status(ToolStatus.AVAILABLE)
                    .branch(branch)
                    .build();
            toolRepository.save(tool);
        }
    }

    private String clean(String name) {
        return name.replaceAll("[^A-Za-zА-Яа-я0-9]", "");
    }

    private String generateInventory(ToolTemplate template, int index) {
        String prefix = clean(template.getName()).toUpperCase();
        prefix = prefix.length() > 3 ? prefix.substring(0, 3) : prefix;
        return prefix + "-" + index;
    }

    private String generateArticle(ToolTemplate template, int index) {
        String prefix = clean(template.getName()).toUpperCase();
        prefix = prefix.length() > 3 ? prefix.substring(0, 3) : prefix;
        return prefix + "-A" + index;
    }
}

