package com.stockpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private long totalProducts;
    private long totalInventoryUnits;
    private BigDecimal totalInventoryValue;
    private long lowStockCount;
    private long outOfStockCount;
    private long pendingPurchaseOrdersCount;
    private long criticalRiskCount;
    private long anomalyCount;

    private List<CategoryValueDto> inventoryValueByCategory;
    private List<WarehouseStockDto> inventoryByWarehouse;
    private List<StockMovementDto> recentStockMovement;
    private List<TopProductDto> topMovingProducts;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryValueDto {
        private String categoryName;
        private long totalUnits;
        private BigDecimal totalValue;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WarehouseStockDto {
        private String warehouseName;
        private String warehouseCode;
        private long availableUnits;
        private long reservedUnits;
        private long damagedUnits;
        private long onHandUnits;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StockMovementDto {
        private String date;
        private long stockIn;
        private long stockOut;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopProductDto {
        private Long productId;
        private String sku;
        private String name;
        private long totalTransactions;
        private long totalQuantityMoved;
    }
}
