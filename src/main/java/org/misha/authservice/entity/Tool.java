package org.misha.authservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    // Инвентарный номер (уникальный)
    @Column(unique = true)
    private String inventoryNumber;

    // Номер экземпляра
    private Integer instanceNumber;

    // Серийный номер (заводской номер)
    private String serialNumber;

    // Статус
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ToolStatus status = ToolStatus.AVAILABLE;

    // Модель инструмента (обязательная ссылка)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ToolTemplate template;

    // Активный договор (null = свободен)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private RentalDocument contract;

    // Бизнес-поля экземпляра
    private String name; // Название
    private String article; // Артикул
    private Double deposit; // Залог
    private Double purchasePrice; // Цена закупки
    private Double dailyPrice; // Цена в сутки
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_point_id")
    private RentalPoint rentalPoint;

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