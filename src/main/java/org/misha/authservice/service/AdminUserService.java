package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.UserSearchResultDto;
import org.misha.authservice.entity.Role;
import org.misha.authservice.entity.Tag;
import org.misha.authservice.entity.User;
import org.misha.authservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public List<UserSearchResultDto> advancedSearch(
            String query,
            String tagsParam,
            Boolean hasDocuments,
            Integer minDocs,
            Integer maxDocs,
            Boolean simpleMode,
            Boolean consentPersonalData,
            String contractNumber,
            String sort,
            String direction
    ) {

        List<User> users = userRepository.findByRole(Role.ADMIN);

        // 🔍 full-text search
        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase();
            users = users.stream()
                    .filter(u ->
                            contains(u.getFullName(), q) ||
                                    contains(u.getEmail(), q) ||
                                    contains(u.getPhone(), q)
                    ).toList();
        }

        // 🏷 Tag filtering
        if (tagsParam != null && !tagsParam.isBlank()) {
            List<Tag> required = Arrays.stream(tagsParam.split(","))
                    .map(String::trim)
                    .map(Tag::valueOf)
                    .toList();

            users = users.stream()
                    .filter(u -> u.getTags().containsAll(required))
                    .toList();
        }

        // 📄 Has documents? - У админов нет документов, фильтр игнорируется
        // 📄 Min/Max docs - У админов нет документов, фильтры игнорируются

        // 👤 Filter by simple mode
        if (simpleMode != null) {
            users = users.stream()
                    .filter(u -> u.isSimpleMode() == simpleMode)
                    .toList();
        }

        // 🔐 Filter by consent
        if (consentPersonalData != null) {
            users = users.stream()
                    .filter(u -> u.isConsentPersonalData() == consentPersonalData)
                    .toList();
        }

        // 🔎 Search by contract number - У админов нет документов, фильтр игнорируется

        // 📚 Sorting
        Comparator<User> comparator = switch (sort) {
            case "name" -> Comparator.comparing(User::getFullName, Comparator.nullsLast(String::compareTo));
            case "email" -> Comparator.comparing(User::getEmail, Comparator.nullsLast(String::compareTo));
            default -> Comparator.comparing(User::getId);
        };

        if (direction.equals("desc"))
            comparator = comparator.reversed();

        users = users.stream().sorted(comparator).toList();

        // DTO
        return users.stream()
                .map(this::toDto)
                .toList();
    }


    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    private UserSearchResultDto toDto(User user) {
        return new UserSearchResultDto(
                user.getId(),
                defaultString(user.getFullName()),
                defaultString(user.getEmail()),
                defaultString(user.getPhone()),
                user.getTags() != null ? user.getTags() : List.of()
        );
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
