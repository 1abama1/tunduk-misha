package org.misha.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tool_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<ToolTemplate> templates = new ArrayList<>();
}