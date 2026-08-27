package com.stockpilot.service;

import com.stockpilot.dto.response.DashboardSummaryResponse;

import java.util.List;

public interface DashboardService {
    DashboardSummaryResponse getDashboardSummary();
    List<DashboardSummaryResponse.StockMovementDto> getStockMovementChart();
    List<DashboardSummaryResponse.CategoryValueDto> getInventoryByCategoryChart();
    List<DashboardSummaryResponse.WarehouseStockDto> getInventoryByWarehouseChart();
    List<DashboardSummaryResponse.TopProductDto> getTopMovingProductsChart();
}
