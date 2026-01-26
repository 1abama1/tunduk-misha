package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ContractSyncDto;
import org.misha.authservice.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/contracts")
    public ResponseEntity<ContractSyncDto.SyncResponse> syncContracts(@RequestBody ContractSyncDto syncDto) {
        return ResponseEntity.ok(syncService.syncContracts(syncDto));
    }
}
