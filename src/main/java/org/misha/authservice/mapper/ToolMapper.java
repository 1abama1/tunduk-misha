package org.misha.authservice.mapper;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ToolListDto;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.entity.ToolStatus;
import org.misha.authservice.service.ToolRentalGuard;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ToolMapper {

    private final ToolRentalGuard toolRentalGuard;

    public ToolListDto toListDto(Tool tool) {
        ToolStatus status = toolRentalGuard.resolveStatus(tool);

        return new ToolListDto(
                tool.getId(),
                tool.getName(), // ✅ Используем name из инструмента
                tool.getSerialNumber(), // используется как inventoryNumber
                status,
                tool.getTemplate() != null && tool.getTemplate().getCategory() != null
                        ? tool.getTemplate().getCategory().getName() : null,
                tool.getDeposit() != null ? BigDecimal.valueOf(tool.getDeposit()) : null
        );
    }
}

