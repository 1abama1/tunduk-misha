package org.misha.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.misha.authservice.entity.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDto {
    private Long id;
    private String name;
    private String inventoryNumber;
    private Integer instanceNumber;
    private String status;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal deposit;
    private BigDecimal dailyPrice;
    private LocalDateTime createdAt;
    private UUID activeBookingId;
    private ToolTemplateDto template;

    public static ToolDto fromEntity(ToolInstance t) {
        return fromEntity(t, null);
    }

    public static ToolDto fromEntity(ToolInstance t, UUID activeBookingId) {
        ToolDto dto = new ToolDto();
        dto.setId(t.getId());
        dto.setActiveBookingId(activeBookingId);
        dto.setInventoryNumber(t.getInventoryNumber());
        dto.setInstanceNumber(t.getInstanceNumber());
        dto.setCreatedAt(t.getCreatedAt());
        
        if (t.getContract() != null) {
            dto.setStatus("RENTED");
        } else if (activeBookingId != null) {
            dto.setStatus("BOOKED");
        } else {
            dto.setStatus(t.getStatus() != null ? t.getStatus().name() : "AVAILABLE");
        }

        if (t.getTemplate() != null) {
            dto.setName(t.getTemplate().getName());
            dto.setDeposit(t.getTemplate().getDepositAmount());
            dto.setDailyPrice(t.getTemplate().getDailyRentalPrice());
            
            ToolTemplateDto templateDto = new ToolTemplateDto();
            templateDto.setId(t.getTemplate().getId());
            templateDto.setName(t.getTemplate().getName());
            dto.setTemplate(templateDto);

            if (t.getTemplate().getCategory() != null) {
                dto.setCategoryId(t.getTemplate().getCategory().getId());
                dto.setCategoryName(t.getTemplate().getCategory().getName());
            }
        }
        return dto;
    }
}
