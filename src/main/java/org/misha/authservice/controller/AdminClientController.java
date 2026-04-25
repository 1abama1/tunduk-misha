package org.misha.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ClientDto;
import org.misha.authservice.dto.ClientLightSearchDto;
import org.misha.authservice.dto.ClientSearchResultDto;
import org.misha.authservice.dto.CreateClientRequest;
import org.misha.authservice.dto.RentalDocumentDto;
import org.misha.authservice.dto.UpdateClientRequest;
import org.misha.authservice.entity.ClientImage;
import org.misha.authservice.service.AdminClientService;
import org.misha.authservice.service.ClientCardService;
import org.misha.authservice.service.ClientImageService;
import org.misha.authservice.service.ClientService;
import org.misha.authservice.service.ExcelClientImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/clients")
@RequiredArgsConstructor
public class AdminClientController {

    private final AdminClientService adminClientService;
    private final ClientService clientService;
    private final ClientCardService clientCardService;
    private final ClientImageService clientImageService;
    private final ExcelClientImportService excelClientImportService;

    @GetMapping("/search")
    public ResponseEntity<List<ClientSearchResultDto>> searchClients(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean hasDocuments,
            @RequestParam(required = false) Integer minDocs,
            @RequestParam(required = false) Integer maxDocs,
            @RequestParam(required = false) String contractNumber,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        List<ClientSearchResultDto> results = adminClientService.advancedSearch(
                query, tag, hasDocuments, minDocs, maxDocs,
                null, null, contractNumber, sort, direction
        );
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/light")
    public ResponseEntity<List<ClientLightSearchDto>> searchClientsLight(
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(clientService.search(query));
    }

    @PostMapping("/create")
    public ResponseEntity<ClientDto> createClient(@Valid @RequestBody CreateClientRequest req) {
        return ResponseEntity.ok(clientService.create(req));
    }

    @PostMapping("/import/excel")
    public ResponseEntity<ClientDto> importFromExcel(@RequestParam MultipartFile file) throws java.io.IOException {
        return ResponseEntity.ok(excelClientImportService.importClient(file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> getClient(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.get(id));
    }

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<ClientDto>> getAllClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(clientService.getAll(page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDto> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClientRequest req
    ) {
        return ResponseEntity.ok(clientService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteClient(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Клиент удалён"));
    }

    @GetMapping("/{clientId}/documents")
    public ResponseEntity<List<RentalDocumentDto>> getClientDocuments(@PathVariable Long clientId) {
        return ResponseEntity.ok(adminClientService.getClientDocuments(clientId));
    }

    @GetMapping("/{clientId}/contracts/active")
    public ResponseEntity<List<RentalDocumentDto>> getActiveContracts(@PathVariable Long clientId) {
        return ResponseEntity.ok(adminClientService.getActiveContracts(clientId));
    }

    @GetMapping("/{id}/card")
    public ResponseEntity<?> getClientCard(@PathVariable Long id) {
        return ResponseEntity.ok(clientCardService.getClientCard(id));
    }

    @PostMapping("/{clientId}/images")
    public ResponseEntity<Map<String, Integer>> uploadImages(
            @PathVariable Long clientId,
            @RequestParam("files") List<MultipartFile> files
    ) throws java.io.IOException {
        int uploaded = clientImageService.uploadImages(clientId, files);
        return ResponseEntity.ok(Map.of("uploaded", uploaded));
    }

    @GetMapping("/{clientId}/images")
    public ResponseEntity<List<Long>> getImageIds(@PathVariable Long clientId) {
        return ResponseEntity.ok(clientImageService.getImageIds(clientId));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Map<String, String>> deleteImage(@PathVariable Long imageId) {
        clientImageService.deleteImage(imageId);
        return ResponseEntity.ok(Map.of("message", "Фото удалено"));
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long imageId) {
        ClientImage image = clientImageService.getImage(imageId);

        String contentType = image.getFileType();
        MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (image.getFileName() == null ? imageId : image.getFileName()) + "\"")
                .contentType(mediaType)
                .body(image.getData());
    }
}
