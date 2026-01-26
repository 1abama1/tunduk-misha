package org.misha.authservice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.CascadeType;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rental_documents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_contract_number_created_at", columnNames = { "contract_number", "created_at" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String contractNumber;

    private LocalDateTime startDateTime;
    private Double dailyPrice; // Цена за день аренды
    private Double amount; // Общая сумма (totalPrice)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonBackReference
    private Client client;

    private LocalDateTime createdAt;
    private LocalDateTime returnDate;
    private LocalDateTime terminatedAt;
    private String terminationReason;

    @Column(name = "tool_id")
    private Long toolId;

    private LocalDateTime updatedAt;

    @Column(name = "offline_id")
    private String offlineId;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Tool> tools = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Вычисляет статус договора на основе returnDate и terminatedAt
     * 
     * @return ACTIVE, CLOSED или TERMINATED
     */
    public ContractStatus getStatus() {
        if (returnDate != null || terminatedAt != null) {
            return ContractStatus.CLOSED;
        }
        return ContractStatus.ACTIVE;
    }
}