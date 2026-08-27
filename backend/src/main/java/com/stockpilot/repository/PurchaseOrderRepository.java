package com.stockpilot.repository;

import com.stockpilot.entity.PurchaseOrder;
import com.stockpilot.entity.PurchaseOrderStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findByWarehouseId(Long warehouseId);

    List<PurchaseOrder> findByStatus(PurchaseOrderStatusEnum status);

    long countByStatus(PurchaseOrderStatusEnum status);
}
