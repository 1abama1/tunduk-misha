package org.misha.authservice.mapper;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ToolListDto;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.service.ToolRentalGuard;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToolMapper {
    private final ToolRentalGuard toolRentalGuard;
    
    public ToolListDto toListDto(ToolInstance tool) {
        String status = toolRentalGuard.resolveStatus(tool);
        return new ToolListDto(
                tool.getId(),
                tool.getTemplate() != null ? tool.getTemplate().getName() : "",
                tool.getInventoryNumber(),
                tool.getInstanceNumber(),
                status,
                (tool.getTemplate() != null && tool.getTemplate().getCategory() != null)
                        ? tool.getTemplate().getCategory().getName() : null,
                tool.getTemplate() != null ? tool.getTemplate().getDepositAmount() : null
        );
    }
}
