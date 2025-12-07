package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.UserSearchResultDto;
import org.misha.authservice.entity.Role;
import org.misha.authservice.entity.Tag;
import org.misha.authservice.entity.User;
import org.misha.authservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
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
        // Парсинг тегов
        final List<Tag> finalTagsList = parseTags(tagsParam);

        // SQL-фильтрация на уровне базы данных
        List<User> users = userRepository.searchAdvanced(
                Role.ADMIN,
                query != null && !query.isBlank() ? query : null,
                simpleMode,
                consentPersonalData
        );

        // Фильтрация по тегам (выполняется в памяти, т.к. JPQL не поддерживает сложные проверки коллекций)
        if (finalTagsList != null && !finalTagsList.isEmpty()) {
            users = users.stream()
                    .filter(u -> u.getTags() != null && u.getTags().containsAll(finalTagsList))
                    .toList();
        }

        // Сортировка (можно также вынести в SQL)
        Comparator<User> baseComparator = switch (sort) {
            case "name" -> Comparator.comparing(User::getFullName, Comparator.nullsLast(String::compareTo));
            case "email" -> Comparator.comparing(User::getEmail, Comparator.nullsLast(String::compareTo));
            default -> Comparator.comparing(User::getId);
        };

        final Comparator<User> finalComparator = (direction != null && direction.equals("desc"))
                ? baseComparator.reversed()
                : baseComparator;

        users = users.stream().sorted(finalComparator).toList();

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

    private List<Tag> parseTags(String tagsParam) {
        if (tagsParam == null || tagsParam.isBlank()) {
            return null;
        }
        try {
            return Arrays.stream(tagsParam.split(","))
                    .map(String::trim)
                    .map(Tag::valueOf)
                    .toList();
        } catch (IllegalArgumentException e) {
            // Игнорируем неверные теги
            return List.of();
        }
    }
}
