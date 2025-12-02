package org.misha.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tool_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ToolCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Boolean available = true;

    @Column(nullable = false)
    private Integer totalCount = 0;

    @Column(nullable = false)
    private Integer availableCount = 0;
}