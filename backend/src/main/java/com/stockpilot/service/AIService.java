package com.stockpilot.service;

import com.stockpilot.dto.request.AIChatRequest;
import com.stockpilot.dto.response.AIChatResponse;
import com.stockpilot.dto.response.AIDailyBriefResponse;

public interface AIService {
    AIDailyBriefResponse generateDailyBrief();
    AIChatResponse processChatQuery(AIChatRequest request, Long userId);
    AIChatResponse explainProductInventory(Long productId);
}
