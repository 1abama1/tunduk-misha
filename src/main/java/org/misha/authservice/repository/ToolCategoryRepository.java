package org.misha.authservice.repository;

import org.misha.authservice.entity.ToolCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ToolCategoryRepository extends JpaRepository<ToolCategory, Long> {
    Optional<ToolCategory> findByName(String name);
}