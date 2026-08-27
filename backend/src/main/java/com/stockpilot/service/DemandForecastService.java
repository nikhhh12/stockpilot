package com.stockpilot.service;

import com.stockpilot.dto.response.DemandForecastResponse;

import java.util.List;

public interface DemandForecastService {
    DemandForecastResponse getProductDemandForecast(Long productId, String algorithm, String horizon);
    List<DemandForecastResponse> getAllDemandForecasts(String horizon);
}
