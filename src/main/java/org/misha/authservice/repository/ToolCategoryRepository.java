package org.misha.authservice.repository;

import org.misha.authservice.entity.ToolCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ToolCategoryRepository extends JpaRepository<ToolCategory, Long> {
    Optional<ToolCategory> findByName(String name);

    @EntityGraph(attributePaths = {"templates", "templates.tools"})
    @Query("SELECT c FROM ToolCategory c WHERE c.id = :id")
    Optional<ToolCategory> findByIdWithTemplatesAndTools(@Param("id") Long id);

    @EntityGraph(attributePaths = {"templates", "templates.tools"})
    @Query("SELECT c FROM ToolCategory c")
    List<ToolCategory> findAllWithTemplatesAndTools();
}