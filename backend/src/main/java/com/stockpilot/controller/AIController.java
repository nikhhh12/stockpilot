package com.stockpilot.controller;

import com.stockpilot.dto.request.AIChatRequest;
import com.stockpilot.dto.response.AIChatResponse;
import com.stockpilot.dto.response.AIDailyBriefResponse;
import com.stockpilot.dto.response.ApiResponse;
import com.stockpilot.service.AIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @GetMapping("/daily-brief")
    public ResponseEntity<ApiResponse<AIDailyBriefResponse>> getDailyBrief() {
        AIDailyBriefResponse response = aiService.generateDailyBrief();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AIChatResponse>> chatWithCopilot(@Valid @RequestBody AIChatRequest request) {
        AIChatResponse response = aiService.processChatQuery(request, null);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/explain/{productId}")
    public ResponseEntity<ApiResponse<AIChatResponse>> explainProductInventory(@PathVariable Long productId) {
        AIChatResponse response = aiService.explainProductInventory(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
