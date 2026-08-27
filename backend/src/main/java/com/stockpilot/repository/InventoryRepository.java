package com.stockpilot.repository;

import com.stockpilot.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    List<Inventory> findByProductId(Long productId);

    List<Inventory> findByWarehouseId(Long warehouseId);

    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.product.reorderLevel")
    List<Inventory> findLowStockInventories();

    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity = 0")
    List<Inventory> findOutOfStockInventories();

    @Query("SELECT SUM(i.availableQuantity + i.reservedQuantity + i.damagedQuantity) FROM Inventory i WHERE i.product.id = :productId")
    Long sumTotalOnHandQuantityByProduct(@Param("productId") Long productId);
}
