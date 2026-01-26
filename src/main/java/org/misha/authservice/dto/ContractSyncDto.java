package org.misha.authservice.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ContractSyncDto {

    private List<CreateItem> creations;
    private List<UpdateItem> updates;
    private List<CloseItem> closures;

    @Data
    public static class CreateItem {
        private String offlineId;
        private Long clientId;
        private Long toolId;
        private String contractNumber;
        private LocalDateTime startDateTime;
    }

    @Data
    public static class UpdateItem {
        private Long id;
        private String offlineId;
        private String comment;
    }

    @Data
    public static class CloseItem {
        private Long id;
        private String offlineId;
        private Double paidAmount;
        private String comment;
    }

    @Data
    @Builder
    public static class SyncResponse {
        private List<IdMapping> idMappings;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IdMapping {
        private String offlineId;
        private Long backendId;
        private String contractNumber;
    }
}
