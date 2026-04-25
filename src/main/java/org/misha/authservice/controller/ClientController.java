package org.misha.authservice.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.client.ClientCreateRequest;
import org.misha.authservice.dto.client.ClientResponseDto;
import org.misha.authservice.service.ClientDirectoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientDirectoryService clientService;

    @PostMapping
    public ClientResponseDto create(@Valid @RequestBody ClientCreateRequest dto) {
        return clientService.create(dto);
    }

    @GetMapping("/search")
    public List<ClientResponseDto> search(@RequestParam String q) {
        return clientService.search(q);
    }

    @GetMapping("/{id}")
    public ClientResponseDto getById(@PathVariable Long id) {
        return clientService.getById(id);
    }

    @PostMapping("/{id}/tags/{tag}")
    public void addTag(@PathVariable Long id, @PathVariable String tag) {
        clientService.addTag(id, tag);
    }

    @DeleteMapping("/{id}/tags/{tag}")
    public void removeTag(@PathVariable Long id, @PathVariable String tag) {
        clientService.removeTag(id, tag);
    }
}

