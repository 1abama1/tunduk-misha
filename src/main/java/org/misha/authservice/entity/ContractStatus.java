package org.misha.authservice.entity;

public enum ContractStatus {
    ACTIVE,      // Активен: closedAt == null && terminatedAt == null && expectedReturnDate >= today
    OVERDUE,     // Просрочен: closedAt == null && terminatedAt == null && expectedReturnDate < today
    CLOSED,      // Закрыт: closedAt != null
    TERMINATED   // Расторгнут: terminatedAt != null
}

