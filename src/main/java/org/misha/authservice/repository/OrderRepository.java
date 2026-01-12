package org.misha.authservice.repository;

import org.misha.authservice.entity.Order;
import org.misha.authservice.entity.Trader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByTrader(Trader trader, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.trader = :trader AND o.updatedAt > :since ORDER BY o.updatedAt ASC")
    Page<Order> findByTraderAndUpdatedAfter(@Param("trader") Trader trader, 
                                              @Param("since") OffsetDateTime since, 
                                              Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.trader = :trader AND o.version > :version ORDER BY o.version ASC")
    Page<Order> findByTraderAndVersionGreaterThan(@Param("trader") Trader trader, 
                                                   @Param("version") Long version, 
                                                   Pageable pageable);
}

