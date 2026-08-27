package com.stockpilot.repository;

import com.stockpilot.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long>, JpaSpecificationExecutor<PurchaseOrderItem> {

    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);

    @Query("SELECT SUM(poi.orderedQuantity - poi.receivedQuantity) FROM PurchaseOrderItem poi WHERE poi.product.id = :productId AND poi.purchaseOrder.status IN ('APPROVED', 'ORDERED', 'PARTIALLY_RECEIVED')")
    Long sumPipelineOnOrderQuantityByProduct(@Param("productId") Long productId);
}
