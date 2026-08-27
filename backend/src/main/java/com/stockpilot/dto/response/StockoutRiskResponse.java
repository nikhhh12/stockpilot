package com.stockpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockoutRiskResponse {
    private Long productId;
    private String productSku;
    private String productName;
    private Integer availableQuantity; // Fulfillable usable stock
    private Double averageDailyDemand;
    private Double estimatedDaysRemaining;
    private Integer reorderLevel;
    private String riskLevel; // LOW (>30d), MEDIUM (15-30d), HIGH (7-14d), CRITICAL (<=6d)
    private String explanation;
}
