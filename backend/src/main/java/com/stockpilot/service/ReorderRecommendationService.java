package com.stockpilot.service;

import com.stockpilot.dto.response.ReorderRecommendationResponse;

import java.util.List;

public interface ReorderRecommendationService {
    List<ReorderRecommendationResponse> getReorderRecommendations();
    ReorderRecommendationResponse getProductReorderRecommendation(Long productId);
}
