package com.stockpilot.service.impl;

import com.stockpilot.dto.request.AIChatRequest;
import com.stockpilot.dto.response.*;
import com.stockpilot.entity.*;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.repository.*;
import com.stockpilot.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceImpl implements AIService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final CategoryRepository categoryRepository;

    private final StockoutRiskService stockoutRiskService;
    private final ReorderRecommendationService reorderRecommendationService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final DemandForecastService demandForecastService;
    private final DashboardService dashboardService;
    private final com.stockpilot.service.ai.LlmClient llmClient;

    @Value("${stockpilot.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${stockpilot.ai.provider:openai}")
    private String aiProvider;

    @Value("${stockpilot.ai.model:gpt-4o-mini}")
    private String aiModel;

    @Override
    @Transactional(readOnly = true)
    public AIDailyBriefResponse generateDailyBrief() {
        DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
        List<StockoutRiskResponse> criticalRisks = stockoutRiskService.getCriticalStockoutRisks();
        List<ReorderRecommendationResponse> recommendations = reorderRecommendationService.getReorderRecommendations();
        List<AnomalyDetectionResponse> anomalies = anomalyDetectionService.detectTransactionAnomalies();

        List<Inventory> inventories = inventoryRepository.findAll();
        long avail = inventories.stream().mapToLong(i -> i.getAvailableQuantity() != null ? i.getAvailableQuantity() : 0).sum();
        long res = inventories.stream().mapToLong(i -> i.getReservedQuantity() != null ? i.getReservedQuantity() : 0).sum();
        long dam = inventories.stream().mapToLong(i -> i.getDamagedQuantity() != null ? i.getDamagedQuantity() : 0).sum();
        long onHand = inventories.stream().mapToLong(Inventory::getOnHandQuantity).sum();

        AIDailyBriefResponse.FactSummary facts = AIDailyBriefResponse.FactSummary.builder()
                .totalProducts(summary.getTotalProducts())
                .lowStockCount(summary.getLowStockCount())
                .outOfStockCount(summary.getOutOfStockCount())
                .criticalRiskCount(criticalRisks.size())
                .anomalyCount(anomalies.size())
                .pendingPurchaseOrdersCount(summary.getPendingPurchaseOrdersCount())
                .build();

        AIDailyBriefResponse.CalculationSummary calculations = AIDailyBriefResponse.CalculationSummary.builder()
                .totalAvailableUnits(avail)
                .totalReservedUnits(res)
                .totalDamagedUnits(dam)
                .totalOnHandUnits(onHand)
                .totalInventoryValue(summary.getTotalInventoryValue())
                .build();

        List<String> highPriorityRisks = new ArrayList<>();
        for (StockoutRiskResponse risk : criticalRisks.stream().limit(3).collect(Collectors.toList())) {
            highPriorityRisks.add(String.format("Critical Stockout: SKU '%s' (%s) has %.1f days of supply remaining.",
                    risk.getProductSku(), risk.getProductName(), risk.getEstimatedDaysRemaining()));
        }
        for (AnomalyDetectionResponse anom : anomalies.stream().limit(2).collect(Collectors.toList())) {
            highPriorityRisks.add(String.format("Consumption Outlier: Transaction #%d on SKU '%s' (Z-Score: %.2f, Quantity: %d).",
                    anom.getTransactionId(), anom.getProductSku(), anom.getAbsoluteZScore(), anom.getObservedQuantity()));
        }

        List<String> actions = new ArrayList<>();
        if (!recommendations.isEmpty()) {
            ReorderRecommendationResponse topRec = recommendations.get(0);
            actions.add(String.format("Replenishment: Issue purchase order for %d units of '%s' from %s (Est. Cost: $%.2f).",
                    topRec.getRecommendedReorderQuantity(), topRec.getProductSku(), topRec.getPreferredSupplierName(), topRec.getEstimatedCost()));
        }
        if (summary.getPendingPurchaseOrdersCount() > 0) {
            actions.add(String.format("Workflow: %d pending purchase orders are awaiting manager approval.", summary.getPendingPurchaseOrdersCount()));
        }
        if (dam > 0) {
            actions.add(String.format("Quality Assurance: %d units are flagged as damaged across fulfillment centers.", dam));
        }

        String execSummary = String.format(
                "StockPilot Intelligence Brief: Managing %d active SKUs across %d fulfillment centers with total physical inventory valuation of $%.2f. " +
                        "Identified %d critical stockout risks and %d active reorder recommendations requiring attention.",
                summary.getTotalProducts(), warehouseRepository.count(), summary.getTotalInventoryValue(),
                criticalRisks.size(), recommendations.size()
        );

        AIDailyBriefResponse.AIInterpretation interpretation = AIDailyBriefResponse.AIInterpretation.builder()
                .executiveSummary(execSummary)
                .highPriorityRisks(highPriorityRisks)
                .recommendedActions(actions)
                .build();

        return AIDailyBriefResponse.builder()
                .facts(facts)
                .calculations(calculations)
                .aiInterpretation(interpretation)
                .providerAvailable(llmClient.isLiveProviderAvailable())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AIChatResponse processChatQuery(AIChatRequest request, Long userId) {
        String query = request.getMessage().toLowerCase().trim();
        String conversationId = request.getConversationId() != null ? request.getConversationId() : UUID.randomUUID().toString();
        List<String> toolsInvoked = new ArrayList<>();

        StringBuilder response = new StringBuilder();

        if (query.contains("risk") || query.contains("stockout") || query.contains("run out") || query.contains("critical")) {
            toolsInvoked.add("getStockoutRisks()");
            List<StockoutRiskResponse> risks = stockoutRiskService.getCriticalStockoutRisks();
            response.append("### ⚠️ Critical Stockout Risk Analysis\n\n");
            if (risks.isEmpty()) {
                response.append("All inventory levels are currently within safe buffer thresholds.\n");
            } else {
                response.append(String.format("Identified **%d products** with critical or high risk of stockout:\n\n", risks.size()));
                for (StockoutRiskResponse r : risks.stream().limit(5).collect(Collectors.toList())) {
                    response.append(String.format("- **%s** (`%s`): Available: **%d units** | Estimated Days Remaining: **%.1f days** | Status: `%s`\n",
                            r.getProductName(), r.getProductSku(), r.getAvailableQuantity(), r.getEstimatedDaysRemaining(), r.getRiskLevel()));
                }
            }
        } else if (query.contains("reorder") || query.contains("recommend") || query.contains("purchase order") || query.contains("po")) {
            toolsInvoked.add("getReorderRecommendations()");
            toolsInvoked.add("getPurchaseOrderSummary()");
            List<ReorderRecommendationResponse> recs = reorderRecommendationService.getReorderRecommendations();
            response.append("### 📦 Reorder & Replenishment Recommendations\n\n");
            if (recs.isEmpty()) {
                response.append("No immediate replenishment orders required. Pipeline inventory is sufficient.\n");
            } else {
                response.append(String.format("Calculated **%d replenishment recommendations** based on lead time and run-rate:\n\n", recs.size()));
                for (ReorderRecommendationResponse r : recs.stream().limit(5).collect(Collectors.toList())) {
                    response.append(String.format("- **%s** (`%s`): Reorder **%d units** from *%s* (Est: $%.2f) — Urgency: `%s`\n",
                            r.getProductName(), r.getProductSku(), r.getRecommendedReorderQuantity(), r.getPreferredSupplierName(), r.getEstimatedCost(), r.getUrgency()));
                }
            }
        } else if (query.contains("anomal") || query.contains("outlier") || query.contains("unusual") || query.contains("spike")) {
            toolsInvoked.add("getAnomalies()");
            List<AnomalyDetectionResponse> anomalies = anomalyDetectionService.detectTransactionAnomalies();
            response.append("### 🔍 Z-Score Statistical Anomaly Detection\n\n");
            if (anomalies.isEmpty()) {
                response.append("No statistical transaction anomalies (Z-Score ≥ 2.0) detected in recent stock activity.\n");
            } else {
                response.append(String.format("Detected **%d consumption anomalies** exceeding standard statistical thresholds:\n\n", anomalies.size()));
                for (AnomalyDetectionResponse a : anomalies.stream().limit(5).collect(Collectors.toList())) {
                    response.append(String.format("- Transaction #%d on **%s** (`%s`): Quantity = **%d** (Expected Mean: %.1f, Z-Score: **%.2f**) — Severity: `%s`\n",
                            a.getTransactionId(), a.getProductName(), a.getProductSku(), a.getObservedQuantity(), a.getExpectedMeanQuantity(), a.getAbsoluteZScore(), a.getSeverity()));
                }
            }
        } else if (query.contains("warehouse") || query.contains("location") || query.contains("hub")) {
            toolsInvoked.add("getInventoryByWarehouse()");
            List<DashboardSummaryResponse.WarehouseStockDto> whs = dashboardService.getInventoryByWarehouseChart();
            response.append("### 🏭 Warehouse Inventory Distribution\n\n");
            for (DashboardSummaryResponse.WarehouseStockDto w : whs) {
                response.append(String.format("- **%s** (`%s`): Total On-Hand: **%d units** (Available: %d, Reserved: %d, Damaged: %d)\n",
                        w.getWarehouseName(), w.getWarehouseCode(), w.getOnHandUnits(), w.getAvailableUnits(), w.getReservedUnits(), w.getDamagedUnits()));
            }
        } else {
            toolsInvoked.add("getInventorySummary()");
            DashboardSummaryResponse sum = dashboardService.getDashboardSummary();
            response.append("### 📊 StockPilot Inventory Overview\n\n");
            response.append(String.format("- **Active Catalog:** %d products\n", sum.getTotalProducts()));
            response.append(String.format("- **Physical Inventory:** %d units across all warehouses\n", sum.getTotalInventoryUnits()));
            response.append(String.format("- **Total Valuation:** $%.2f\n", sum.getTotalInventoryValue()));
            response.append(String.format("- **Low Stock Alerts:** %d SKUs\n", sum.getLowStockCount()));
            response.append(String.format("- **Out of Stock:** %d SKUs\n", sum.getOutOfStockCount()));
            response.append(String.format("- **Pending Purchase Orders:** %d\n", sum.getPendingPurchaseOrdersCount()));
            response.append("\n*Tip: Ask me about 'critical stockout risks', 'reorder recommendations', 'statistical anomalies', or 'warehouse stock distribution'.*");
        }

        String deterministicReply = response.toString();
        String finalReply = deterministicReply;

        if (llmClient.isLiveProviderAvailable()) {
            String systemPrompt = "You are StockPilot AI, a supply chain and inventory management assistant. " +
                    "Answer the user's inquiry professionally using ONLY the following authoritative system metrics and tool data. " +
                    "Do NOT fabricate numbers or inventory levels. You are strictly read-only: refuse any requests to mutate data, execute SQL, or approve orders, " +
                    "and guide the user to the proper UI workflows.\n\n" +
                    "Authoritative System Tool Data:\n" + deterministicReply;

            Optional<String> llmOutput = llmClient.generateGroundedCompletion(systemPrompt, request.getMessage());
            if (llmOutput.isPresent() && !llmOutput.get().trim().isEmpty()) {
                finalReply = llmOutput.get();
            }
        }

        return AIChatResponse.builder()
                .reply(finalReply)
                .conversationId(conversationId)
                .toolsInvoked(toolsInvoked)
                .providerAvailable(llmClient.isLiveProviderAvailable())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AIChatResponse explainProductInventory(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        StockoutRiskResponse risk = stockoutRiskService.calculateProductStockoutRisk(productId);
        DemandForecastResponse forecast = demandForecastService.getProductDemandForecast(productId, "MOVING_AVERAGE", "14_DAYS");
        ReorderRecommendationResponse reorder = reorderRecommendationService.getProductReorderRecommendation(productId);
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);

        int totalAvail = inventories.stream().mapToInt(i -> i.getAvailableQuantity() != null ? i.getAvailableQuantity() : 0).sum();
        int totalRes = inventories.stream().mapToInt(i -> i.getReservedQuantity() != null ? i.getReservedQuantity() : 0).sum();
        int totalDam = inventories.stream().mapToInt(i -> i.getDamagedQuantity() != null ? i.getDamagedQuantity() : 0).sum();
        int totalOnHand = totalAvail + totalRes + totalDam;

        String deterministicExplanation = String.format(
                "### 🤖 StockPilot AI Explanation for %s (`%s`)\n\n" +
                        "**Inventory Status:**\n" +
                        "- **Available:** %d units (Fulfillable)\n" +
                        "- **Reserved:** %d units (Committed to active orders)\n" +
                        "- **Damaged:** %d units (Quarantined)\n" +
                        "- **Total Physical On-Hand:** %d units\n\n" +
                        "**Operational Risk Assessment:**\n" +
                        "- **Risk Level:** `%s` (Estimated **%.1f days** of stock remaining)\n" +
                        "- **Average Daily Demand:** %.1f units/day\n" +
                        "- **14-Day Demand Forecast:** %.1f units\n\n" +
                        "**Recommendation:**\n" +
                        "%s",
                product.getName(), product.getSku(),
                totalAvail, totalRes, totalDam, totalOnHand,
                risk.getRiskLevel(), risk.getEstimatedDaysRemaining(),
                risk.getAverageDailyDemand(), forecast.getPredictedDemand(),
                reorder.getUrgency().equals("LOW")
                        ? "✅ Current stock levels are sufficient. No replenishment needed at this time."
                        : String.format("⚠️ Recommended to reorder **%d units** from %s (Urgency: `%s`). Reason: %s",
                        reorder.getRecommendedReorderQuantity(), reorder.getPreferredSupplierName(), reorder.getUrgency(), reorder.getReason())
        );

        String finalExplanation = deterministicExplanation;
        if (llmClient.isLiveProviderAvailable()) {
            String systemPrompt = "You are StockPilot AI. Provide a concise, professional diagnosis of this product's inventory position and risk profile based solely on these verified statistics. Refuse any mutation request.";
            Optional<String> llmOutput = llmClient.generateGroundedCompletion(systemPrompt, deterministicExplanation);
            if (llmOutput.isPresent() && !llmOutput.get().trim().isEmpty()) {
                finalExplanation = llmOutput.get();
            }
        }

        return AIChatResponse.builder()
                .reply(finalExplanation)
                .conversationId(UUID.randomUUID().toString())
                .toolsInvoked(Arrays.asList("getProductStockoutRisk()", "getProductDemandForecast()", "getProductReorderRecommendation()"))
                .providerAvailable(llmClient.isLiveProviderAvailable())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
