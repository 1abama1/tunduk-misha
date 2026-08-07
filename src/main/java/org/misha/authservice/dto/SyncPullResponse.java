package org.misha.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncPullResponse {
    private List<ClientDto> clients;
    private List<ToolDto> tools;
    private List<RentalDocumentDto> documents;
    private List<CategoryDto> categories;
    private List<TemplateDto> templates;
    private long lastSyncTimestamp;
}
