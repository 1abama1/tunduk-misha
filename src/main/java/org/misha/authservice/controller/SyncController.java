package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ContractSyncDto;
import org.misha.authservice.dto.SyncPullResponse;
import org.misha.authservice.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;
    private final org.misha.authservice.service.IdempotencyService idempotency;

    @PostMapping("/contracts")
    public ResponseEntity<ContractSyncDto.SyncResponse> syncContracts(@jakarta.validation.Valid @RequestBody ContractSyncDto syncDto,
            @org.springframework.web.bind.annotation.RequestHeader(value="Idempotency-Key", required=false) String operationKey) {
        return ResponseEntity.ok(idempotency.execute(operationKey, syncDto, ContractSyncDto.SyncResponse.class, () -> syncService.syncContracts(syncDto)));
    }

    @GetMapping("/pull")
    public ResponseEntity<SyncPullResponse> pullSync(
            @RequestParam(value = "since", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.Instant since,
            @RequestParam(value = "branchId") Long branchId) {
        return ResponseEntity.ok(syncService.pullSync(since, branchId));
    }
}
