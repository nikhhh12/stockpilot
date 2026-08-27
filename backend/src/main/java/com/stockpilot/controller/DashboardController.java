package com.stockpilot.controller;

import com.stockpilot.dto.response.ApiResponse;
import com.stockpilot.dto.response.DashboardSummaryResponse;
import com.stockpilot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary() {
        DashboardSummaryResponse response = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stock-movement")
    public ResponseEntity<ApiResponse<List<DashboardSummaryResponse.StockMovementDto>>> getStockMovement() {
        List<DashboardSummaryResponse.StockMovementDto> response = dashboardService.getStockMovementChart();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inventory-by-category")
    public ResponseEntity<ApiResponse<List<DashboardSummaryResponse.CategoryValueDto>>> getInventoryByCategory() {
        List<DashboardSummaryResponse.CategoryValueDto> response = dashboardService.getInventoryByCategoryChart();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inventory-by-warehouse")
    public ResponseEntity<ApiResponse<List<DashboardSummaryResponse.WarehouseStockDto>>> getInventoryByWarehouse() {
        List<DashboardSummaryResponse.WarehouseStockDto> response = dashboardService.getInventoryByWarehouseChart();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/top-moving-products")
    public ResponseEntity<ApiResponse<List<DashboardSummaryResponse.TopProductDto>>> getTopMovingProducts() {
        List<DashboardSummaryResponse.TopProductDto> response = dashboardService.getTopMovingProductsChart();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
