package com.stockpilot.service;

import com.stockpilot.dto.request.*;
import com.stockpilot.dto.response.InventoryResponse;
import com.stockpilot.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {
    InventoryResponse stockIn(StockInRequest request, Long userId);
    InventoryResponse stockOut(StockOutRequest request, Long userId);
    InventoryResponse adjustStock(StockAdjustmentRequest request, Long userId);
    InventoryResponse reserveStock(StockReserveRequest request, Long userId);
    InventoryResponse releaseStock(StockReleaseRequest request, Long userId);
    InventoryResponse damageStock(StockDamageRequest request, Long userId);
    InventoryResponse returnStock(StockReturnRequest request, Long userId);
    List<InventoryResponse> transferStock(StockTransferRequest request, Long userId);

    InventoryResponse getInventoryById(Long id);
    List<InventoryResponse> getInventoryByProduct(Long productId);
    List<InventoryResponse> getInventoryByWarehouse(Long warehouseId);
    List<InventoryResponse> getLowStockInventories();
    List<InventoryResponse> getOutOfStockInventories();

    PagedResponse<InventoryResponse> getInventories(
            String search, Long warehouseId, Long categoryId, String stockStatus, Pageable pageable
    );
}
