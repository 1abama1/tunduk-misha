package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ActiveContractRowDto;
import org.misha.authservice.dto.ContractTableDto;
import org.misha.authservice.entity.ContractStatus;
import org.misha.authservice.service.ContractService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractsController {

    private final ContractService contractService;

    @GetMapping("/active-table")
    public List<ActiveContractRowDto> getActiveContractsTable() {
        return contractService.getActiveContractsTable();
    }

    @GetMapping("/history-table")
    public List<ContractTableDto> getHistoryContractsTable(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long toolId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ContractStatus status
    ) {
        return contractService.getHistoryTable(clientId, toolId, from, to, status);
    }
}

