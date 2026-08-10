package org.misha.authservice.repository;

import org.misha.authservice.entity.ToolCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ToolCategoryRepository extends JpaRepository<ToolCategory, java.util.UUID> {
    List<ToolCategory> findByUpdatedAtAfter(java.time.LocalDateTime since);
    Optional<ToolCategory> findByName(String name);

    @EntityGraph(attributePaths = {"templates", "templates.instances"})
    @Query("SELECT c FROM ToolCategory c WHERE c.id = :id")
    Optional<ToolCategory> findByIdWithTemplatesAndTools(@Param("id") java.util.UUID id);

    @EntityGraph(attributePaths = {"templates", "templates.instances"})
    @Query("SELECT c FROM ToolCategory c")
    List<ToolCategory> findAllWithTemplatesAndTools();
}