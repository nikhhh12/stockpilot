package com.stockpilot.service;

import com.stockpilot.dto.response.StockoutRiskResponse;

import java.util.List;

public interface StockoutRiskService {
    StockoutRiskResponse calculateProductStockoutRisk(Long productId);
    List<StockoutRiskResponse> getAllStockoutRisks();
    List<StockoutRiskResponse> getCriticalStockoutRisks();
}
