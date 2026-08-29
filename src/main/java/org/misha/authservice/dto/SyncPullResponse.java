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
    private List<Long> deletedClientIds;

    private List<ToolDto> tools;
    private List<Long> deletedToolIds;

    @com.fasterxml.jackson.annotation.JsonProperty("contracts")
    private List<RentalDocumentDto> documents;
    
    @com.fasterxml.jackson.annotation.JsonProperty("deletedContractIds")
    private List<Long> deletedDocumentIds;

    private List<CategoryDto> categories;
    private List<java.util.UUID> deletedCategoryIds; // Wait, Category ID is UUID. Prompt says List<Long>, but ToolCategory uses UUID. Let's use List<java.util.UUID> or just List<String>. Let's check ToolCategory.java -> UUID id.

    private List<TemplateDto> templates;
    private List<java.util.UUID> deletedTemplateIds;

    private java.time.Instant serverTimestamp;
    private boolean fullSyncRequired;
}
