package com.stockpilot.repository;

import com.stockpilot.entity.StockTransaction;
import com.stockpilot.entity.TransactionTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long>, JpaSpecificationExecutor<StockTransaction> {

    List<StockTransaction> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<StockTransaction> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);

    List<StockTransaction> findByProductIdAndCreatedAtAfter(Long productId, LocalDateTime after);

    @Query("SELECT st FROM StockTransaction st WHERE st.product.id = :productId AND st.transactionType IN :types AND st.createdAt >= :startDate ORDER BY st.createdAt ASC")
    List<StockTransaction> findDemandTransactionsForProduct(
            @Param("productId") Long productId,
            @Param("types") List<TransactionTypeEnum> types,
            @Param("startDate") LocalDateTime startDate
    );
    boolean existsByReferenceNumberAndTransactionType(String referenceNumber, TransactionTypeEnum transactionType);
    java.util.Optional<StockTransaction> findFirstByReferenceNumberAndTransactionType(String referenceNumber, TransactionTypeEnum transactionType);
}
