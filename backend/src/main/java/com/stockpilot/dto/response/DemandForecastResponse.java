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
public class DemandForecastResponse {
    private Long productId;
    private String productSku;
    private String productName;
    private String forecastHorizon;
    private Double averageDailyDemand;
    private Double predictedDemand;
    private String algorithm; // MOVING_AVERAGE, EXPONENTIAL_SMOOTHING
    private int dataPointsUsed;
    private String status;    // OK, INSUFFICIENT_DATA
    private String message;
    private List<DailyDemandPoint> historicalData;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyDemandPoint {
        private String date;
        private Integer quantity;
    }
}
