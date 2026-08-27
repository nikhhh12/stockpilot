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
public class AnomalyDetectionResponse {
    private Long transactionId;
    private Long productId;
    private String productSku;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private String transactionType;
    private Integer observedQuantity;
    private Double expectedMeanQuantity;
    private Double standardDeviation;
    private Double absoluteZScore;
    private String status;   // STATISTICAL_ANOMALY, NORMAL, NO_VARIANCE, INSUFFICIENT_DATA
    private String severity; // HIGH, MEDIUM, LOW
    private String reason;   // Outlier analysis rationale
    private LocalDateTime transactionDate;
}
