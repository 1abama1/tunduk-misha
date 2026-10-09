package org.misha.authservice.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractSyncDto {

    @jakarta.validation.Valid
    @jakarta.validation.constraints.Size(max=100)
    private List<CreateItem> creations;
    @jakarta.validation.Valid
    @jakarta.validation.constraints.Size(max=100)
    private List<UpdateItem> updates;
    @jakarta.validation.Valid
    @jakarta.validation.constraints.Size(max=100)
    private List<CloseItem> closures;

    @Data
    public static class CreateItem {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max=100)
        private String offlineId;
        private LocalDateTime startDateTime;
        private Long clientId;
        private Long toolId;
        @jakarta.validation.constraints.Size(max=10)
        private List<@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long> toolIds;
        private String contractNumber;
    }

    @Data
    public static class UpdateItem {
        private LocalDateTime expectedUpdatedAt;
        private Long id;
        private String offlineId;
        @jakarta.validation.constraints.Size(max=255)
        private String comment;
    }

    @Data
    public static class CloseItem {
        private LocalDateTime expectedUpdatedAt;
        private Long id;
        private String offlineId;
        @jakarta.validation.constraints.DecimalMin("0.0")
        private Double paidAmount;
        @jakarta.validation.constraints.Size(max=255)
        private String comment;
        private boolean isBroken;
        private LocalDateTime actualReturnDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResponse {
        private List<IdMapping> idMappings;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IdMapping {
        private String offlineId;
        private Long backendId;
        private LocalDateTime updatedAt;
        public IdMapping(String offlineId, Long backendId, String contractNumber) {
            this.offlineId = offlineId; this.backendId = backendId; this.contractNumber = contractNumber;
        }
        private String contractNumber;
    }
}
