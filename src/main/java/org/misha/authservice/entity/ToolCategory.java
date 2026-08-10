package org.misha.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tool_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"templates", "subCategories", "parentCategory"})
public class ToolCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private ToolCategory parentCategory;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @org.hibernate.annotations.UpdateTimestamp
    private java.time.LocalDateTime updatedAt;

    @OneToMany(mappedBy = "category")
    @Builder.Default
    private Set<ToolTemplate> templates = new HashSet<>();

    @OneToMany(mappedBy = "parentCategory")
    @Builder.Default
    private Set<ToolCategory> subCategories = new HashSet<>();
}