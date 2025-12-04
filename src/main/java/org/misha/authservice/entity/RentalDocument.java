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
import jakarta.persistence.CascadeType;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "rental_documents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_contract_number_created_at",
                        columnNames = {"contract_number", "created_at"}
                )
        }
)
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
    private LocalDate expectedReturnDate;
    private Double amount;
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonBackReference
    private Client client;

    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private LocalDateTime terminatedAt;
    private String terminationReason;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Tool> tools = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Вычисляет статус договора на основе closedAt, terminatedAt и expectedReturnDate
     * @return ACTIVE, OVERDUE, CLOSED или TERMINATED
     */
    public ContractStatus getStatus() {
        if (terminatedAt != null) {
            return ContractStatus.TERMINATED;
        }
        if (closedAt != null) {
            return ContractStatus.CLOSED;
        }
        // Проверка просрочки: expectedReturnDate < today AND closedAt == null AND terminatedAt == null
        if (expectedReturnDate != null && expectedReturnDate.isBefore(java.time.LocalDate.now())) {
            return ContractStatus.OVERDUE;
        }
        return ContractStatus.ACTIVE;
    }
}