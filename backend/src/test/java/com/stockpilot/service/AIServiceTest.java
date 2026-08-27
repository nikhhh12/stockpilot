package com.stockpilot.service;

import com.stockpilot.dto.request.AIChatRequest;
import com.stockpilot.dto.response.AIChatResponse;
import com.stockpilot.dto.response.AIDailyBriefResponse;
import com.stockpilot.entity.Product;
import com.stockpilot.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
public class AIServiceTest {

    @Autowired
    private AIService aiService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Daily Brief generates structured fact summary and executive interpretation gracefully")
    void testDailyBriefGeneration() {
        AIDailyBriefResponse brief = aiService.generateDailyBrief();
        assertNotNull(brief);
        assertNotNull(brief.getFacts());
        assertNotNull(brief.getCalculations());
        assertNotNull(brief.getAiInterpretation());
        assertNotNull(brief.getAiInterpretation().getExecutiveSummary());
    }

    @Test
    @DisplayName("AI Chat operates read-only and selects appropriate tools based on query intent")
    void testAIChatToolSelection() {
        AIChatRequest riskQuery = new AIChatRequest();
        riskQuery.setMessage("What are our critical stockout risks today?");

        AIChatResponse riskResponse = aiService.processChatQuery(riskQuery, 1L);
        assertNotNull(riskResponse.getReply());
        assertTrue(riskResponse.getToolsInvoked().contains("getStockoutRisks()"));

        AIChatRequest reorderQuery = new AIChatRequest();
        reorderQuery.setMessage("Show me recommended purchase orders and reorders");

        AIChatResponse reorderResponse = aiService.processChatQuery(reorderQuery, 1L);
        assertNotNull(reorderResponse.getReply());
        assertTrue(reorderResponse.getToolsInvoked().contains("getReorderRecommendations()"));
    }

    @Test
    @DisplayName("AI Product explanation provides comprehensive inventory and risk diagnosis")
    void testAIProductExplanation() {
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        AIChatResponse explanation = aiService.explainProductInventory(product.getId());

        assertNotNull(explanation.getReply());
        assertTrue(explanation.getReply().contains(product.getSku()));
    }
}
