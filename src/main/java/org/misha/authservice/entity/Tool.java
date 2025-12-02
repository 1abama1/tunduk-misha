package org.misha.authservice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tools")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Название
    private String name;

    // Артикул
    private String article;

    // Инвентарный номер
    @Column(unique = true)
    private String inventoryNumber;

    // Статус
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ToolStatus status = ToolStatus.AVAILABLE;

    // Пункт проката
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_point_id")
    private RentalPoint rentalPoint;

    // Категория
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ToolCategory category;

    // Описание
    @Column(columnDefinition = "TEXT")
    private String description;

    // Закупочная цена
    private Double purchasePrice;

    // Дата закупки
    private LocalDate purchaseDate;

    // Денежный залог
    private Double deposit;

    // Активный договор (если есть)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private RentalDocument contract;

    // Шаблон (для обратной совместимости)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ToolTemplate template;

    // Серийный номер (для обратной совместимости)
    private String serialNumber;

    // Параметры инструмента
    @OneToMany(mappedBy = "tool", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ToolAttribute> attributes = new ArrayList<>();

    // Изображения инструмента
    @OneToMany(mappedBy = "tool", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ToolImage> images = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // Автоматически устанавливаем статус RENTED, если есть активный договор
        if (contract != null && status != ToolStatus.RENTED) {
            status = ToolStatus.RENTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Автоматически устанавливаем статус RENTED, если есть активный договор
        if (contract != null && status != ToolStatus.RENTED) {
            status = ToolStatus.RENTED;
        }
        // Если договор закрыт/расторгнут, но статус был RENTED, меняем на AVAILABLE
        if (contract == null && status == ToolStatus.RENTED) {
            status = ToolStatus.AVAILABLE;
        }
    }
}