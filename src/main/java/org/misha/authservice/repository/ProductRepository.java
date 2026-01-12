package org.misha.authservice.repository;

import org.misha.authservice.entity.Product;
import org.misha.authservice.entity.Trader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByTrader(Trader trader, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.trader = :trader AND p.updatedAt > :since ORDER BY p.updatedAt ASC")
    Page<Product> findByTraderAndUpdatedAfter(@Param("trader") Trader trader, 
                                               @Param("since") OffsetDateTime since, 
                                               Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.trader = :trader AND p.version > :version ORDER BY p.version ASC")
    Page<Product> findByTraderAndVersionGreaterThan(@Param("trader") Trader trader, 
                                                      @Param("version") Long version, 
                                                      Pageable pageable);
}

