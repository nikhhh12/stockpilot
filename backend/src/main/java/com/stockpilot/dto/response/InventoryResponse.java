package com.stockpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private String productUnit;
    private Integer reorderLevel;
    private Long warehouseId;
    private String warehouseName;
    private String warehouseCode;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer damagedQuantity;
    private Integer onHandQuantity; // Derived: available + reserved + damaged
    private String stockStatus;     // HEALTHY, LOW_STOCK, OUT_OF_STOCK
    private String stockoutRisk;    // LOW, MEDIUM, HIGH, CRITICAL
    private Long version;
    private LocalDateTime updatedAt;
}
