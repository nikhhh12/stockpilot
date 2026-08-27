package com.stockpilot.controller;

import com.stockpilot.dto.response.*;
import com.stockpilot.service.AnomalyDetectionService;
import com.stockpilot.service.DemandForecastService;
import com.stockpilot.service.ReorderRecommendationService;
import com.stockpilot.service.StockoutRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final DemandForecastService demandForecastService;
    private final StockoutRiskService stockoutRiskService;
    private final ReorderRecommendationService reorderRecommendationService;
    private final AnomalyDetectionService anomalyDetectionService;

    @GetMapping("/forecast/{productId}")
    public ResponseEntity<ApiResponse<DemandForecastResponse>> getProductForecast(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "MOVING_AVERAGE") String algorithm,
            @RequestParam(defaultValue = "14_DAYS") String horizon
    ) {
        DemandForecastResponse response = demandForecastService.getProductDemandForecast(productId, algorithm, horizon);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/forecasts")
    public ResponseEntity<ApiResponse<List<DemandForecastResponse>>> getAllForecasts(
            @RequestParam(defaultValue = "14_DAYS") String horizon
    ) {
        List<DemandForecastResponse> response = demandForecastService.getAllDemandForecasts(horizon);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stockout-risk")
    public ResponseEntity<ApiResponse<List<StockoutRiskResponse>>> getStockoutRisks(
            @RequestParam(defaultValue = "false") boolean criticalOnly
    ) {
        List<StockoutRiskResponse> response = criticalOnly
                ? stockoutRiskService.getCriticalStockoutRisks()
                : stockoutRiskService.getAllStockoutRisks();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stockout-risk/{productId}")
    public ResponseEntity<ApiResponse<StockoutRiskResponse>> getProductStockoutRisk(@PathVariable Long productId) {
        StockoutRiskResponse response = stockoutRiskService.calculateProductStockoutRisk(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reorder-recommendations")
    public ResponseEntity<ApiResponse<List<ReorderRecommendationResponse>>> getReorderRecommendations() {
        List<ReorderRecommendationResponse> response = reorderRecommendationService.getReorderRecommendations();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reorder-recommendations/{productId}")
    public ResponseEntity<ApiResponse<ReorderRecommendationResponse>> getProductReorderRecommendation(@PathVariable Long productId) {
        ReorderRecommendationResponse response = reorderRecommendationService.getProductReorderRecommendation(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/anomalies")
    public ResponseEntity<ApiResponse<List<AnomalyDetectionResponse>>> getTransactionAnomalies() {
        List<AnomalyDetectionResponse> response = anomalyDetectionService.detectTransactionAnomalies();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/anomalies/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<AnomalyDetectionResponse>> analyzeTransactionAnomaly(@PathVariable Long transactionId) {
        AnomalyDetectionResponse response = anomalyDetectionService.analyzeTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
