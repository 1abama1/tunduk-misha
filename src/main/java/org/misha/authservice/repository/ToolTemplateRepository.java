package org.misha.authservice.repository;

import org.misha.authservice.entity.ToolTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolTemplateRepository extends JpaRepository<ToolTemplate, Long> {
    List<ToolTemplate> findByCategoryId(Long categoryId);
}