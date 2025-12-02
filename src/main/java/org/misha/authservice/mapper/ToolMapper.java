package org.misha.authservice.mapper;

import org.misha.authservice.dto.ToolListDto;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.entity.ToolStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class ToolMapper {

    public ToolListDto toListDto(Tool tool) {
        ToolStatus status = resolveStatus(tool);

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

    private ToolStatus resolveStatus(Tool tool) {
        if (tool.getContract() == null) {
            return ToolStatus.AVAILABLE;
        }

        if (tool.getContract().getTerminatedAt() != null) {
            return ToolStatus.AVAILABLE;
        }

        if (tool.getContract().getExpectedReturnDate() != null &&
            tool.getContract().getExpectedReturnDate().isBefore(LocalDate.now())) {
            return ToolStatus.OVERDUE;
        }

        return ToolStatus.RENTED;
    }
}

