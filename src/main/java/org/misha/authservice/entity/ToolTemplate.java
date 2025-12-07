package org.misha.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ToolCategory category;

    @OneToMany(mappedBy = "template")
    @Builder.Default
    private List<Tool> tools = new ArrayList<>();
}