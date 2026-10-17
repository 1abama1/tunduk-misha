package org.misha.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "tool_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"templates", "subCategories", "parentCategory"})
@org.hibernate.annotations.SQLDelete(sql = "UPDATE tool_categories SET is_deleted = true, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@org.hibernate.annotations.SQLRestriction("is_deleted = false")
public class ToolCategory {

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"parentCategory", "subCategories", "templates"})
    private ToolCategory parentCategory;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @org.hibernate.annotations.UpdateTimestamp
    private java.time.LocalDateTime updatedAt;

    @OneToMany(mappedBy = "category")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"category", "instances"})
    @Builder.Default
    private Set<ToolTemplate> templates = new HashSet<>();

    @OneToMany(mappedBy = "parentCategory")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"parentCategory", "subCategories", "templates"})
    @Builder.Default
    private Set<ToolCategory> subCategories = new HashSet<>();
}
