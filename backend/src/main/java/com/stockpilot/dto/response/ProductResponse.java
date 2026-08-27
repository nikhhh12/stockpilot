package com.stockpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long supplierId;
    private String supplierName;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private String unit;
    private Integer reorderLevel;
    private Integer reorderQuantity;
    private boolean active;
    private long totalAvailableStock;
    private long totalReservedStock;
    private long totalDamagedStock;
    private long totalOnHandStock;
    private String stockStatus; // HEALTHY, LOW_STOCK, OUT_OF_STOCK
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
