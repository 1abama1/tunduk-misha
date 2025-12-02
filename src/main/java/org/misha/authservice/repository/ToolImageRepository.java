package org.misha.authservice.repository;

import org.misha.authservice.entity.ToolImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolImageRepository extends JpaRepository<ToolImage, Long> {
    List<ToolImage> findByToolId(Long toolId);
    void deleteByToolId(Long toolId);
}

