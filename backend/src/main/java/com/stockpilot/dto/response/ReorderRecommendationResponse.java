package com.stockpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRecommendationResponse {
    private Long productId;
    private String productSku;
    private String productName;
    private Long preferredSupplierId;
    private String preferredSupplierName;
    private Long warehouseId;
    private String warehouseName;
    private Integer availableStock;
    private Integer reservedStock;
    private Integer pipelineOnOrderQuantity;
    private Double averageDailyDemand;
    private Integer safetyStock;
    private Integer recommendedReorderQuantity;
    private BigDecimal estimatedCost;
    private String urgency; // CRITICAL, HIGH, MEDIUM, LOW
    private String reason;
}
