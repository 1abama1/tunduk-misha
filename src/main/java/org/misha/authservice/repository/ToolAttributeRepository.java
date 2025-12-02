package org.misha.authservice.repository;

import org.misha.authservice.entity.ToolAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolAttributeRepository extends JpaRepository<ToolAttribute, Long> {
    List<ToolAttribute> findByToolId(Long toolId);
    void deleteByToolId(Long toolId);
}

