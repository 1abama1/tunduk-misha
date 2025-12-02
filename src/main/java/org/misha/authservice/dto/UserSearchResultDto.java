package org.misha.authservice.dto;

import org.misha.authservice.entity.Tag;

import java.util.List;

public record UserSearchResultDto(
        Long id,
        String fullName,
        String email,
        String phone,
        List<Tag> tags
) {
}

