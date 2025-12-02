package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.UserSearchResultDto;
import org.misha.authservice.service.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<UserSearchResultDto>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Boolean hasDocuments,
            @RequestParam(required = false) Integer minDocs,
            @RequestParam(required = false) Integer maxDocs,
            @RequestParam(required = false) Boolean simpleMode,
            @RequestParam(required = false) Boolean consentPersonalData,
            @RequestParam(required = false) String contractNumber,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        List<UserSearchResultDto> result = adminUserService.advancedSearch(
                query, tags, hasDocuments, minDocs, maxDocs, simpleMode,
                consentPersonalData, contractNumber, sort, direction
        );
        return ResponseEntity.ok(result);
    }
}


