package org.misha.authservice.entity;

public enum ToolStatus {
    /** Свободен и готов к выдаче */
    AVAILABLE,

    /** Находится у клиента по договору */
    RENTED,

    /** Просрочен — клиент не вернул вовремя */
    OVERDUE,

    /** В ремонте (недоступен для аренды) */
    IN_REPAIR,

    /** Списан — больше не используется */
    DECOMMISSIONED,

    /**
     * @deprecated Используйте {@link #IN_REPAIR}. Оставлен для обратной
     * совместимости с существующими записями в БД.
     */
    @Deprecated
    BROKEN
}

