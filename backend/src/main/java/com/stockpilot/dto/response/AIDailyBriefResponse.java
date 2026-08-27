package com.stockpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIDailyBriefResponse {

    private FactSummary facts;
    private CalculationSummary calculations;
    private AIInterpretation aiInterpretation;
    private boolean providerAvailable;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FactSummary {
        private long totalProducts;
        private long lowStockCount;
        private long outOfStockCount;
        private long criticalRiskCount;
        private long anomalyCount;
        private long pendingPurchaseOrdersCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationSummary {
        private long totalAvailableUnits;
        private long totalReservedUnits;
        private long totalDamagedUnits;
        private long totalOnHandUnits;
        private BigDecimal totalInventoryValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIInterpretation {
        private String executiveSummary;
        private List<String> highPriorityRisks;
        private List<String> recommendedActions;
    }
}
