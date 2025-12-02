package org.misha.authservice.repository;

import org.misha.authservice.entity.ToolCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolCategoryRepository extends JpaRepository<ToolCategory, Long> {
}